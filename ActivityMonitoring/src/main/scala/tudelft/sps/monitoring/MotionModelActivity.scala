package tudelft.sps.monitoring

import android.app.Activity
import android.os.{PersistableBundle, Bundle}
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.{Button, TextView}
import com.androidplot.xy.{BoundaryMode, LineAndPointFormatter, XYPlot, SimpleXYSeries}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import rx.lang.scala.{Subscriber, Observable}
import rx.lang.scala.schedulers.ExecutionContextScheduler
import tudelft.sps.observable.{ManagedSubscriptions, ObservableAccelerometer}
import tudelft.sps.statistics.SeqExtensions._
import tudelft.sps.observable._
import tudelft.sps.data._

import scala.concurrent.ExecutionContext.Implicits._

class MotionModelActivity extends Activity
  with ObservableAccelerometer
  with ManagedSubscriptions
{
  val TAG = "MotionModelActivity"


  var plot:XYPlot = null
  var series:SimpleXYSeries = null


  val tMin = 40
  val tMax = 100

  val autoCorrelation = accelerometer
    .observeOn(ExecutionContextScheduler(global))
    .onBackpressureDrop
    .map(_.magnitude)
    .slidingBuffer(tMax * 2, 5)
    .map{ sample =>
      val t0 = System.currentTimeMillis()

      var t_i = tMin
      var max: (Int, Double, Double) = (0, 0, 0)
      while (t_i <= tMax) {
        val m = tMax * 2 - 2 * t_i
        val mean0 = SeqMath.mean(sample, m, m + t_i)
        val mean1 = SeqMath.mean(sample, m + t_i, m + t_i * 2)

        var k = 0
        var sum:Double = 0
        while(k < t_i){
          sum = sum + (sample(m + k) - mean0) * (sample(m + k + t_i) - mean1)
          k = k + 1
        }
        val stdev1 = SeqMath.stdev(sample, m + t_i, m + t_i * 2)
        val chi = sum / (t_i * SeqMath.stdev(sample, m, m + t_i) * stdev1)
        if(chi > max._2){
          max = (t_i, chi, stdev1)
        }
        t_i = t_i + 1
      }
      val dt = System.currentTimeMillis() - t0
//      Log.d(TAG, "[%s][%dms]autoCorrelation result: (%d, %.2f)".format(Thread.currentThread().getName, dt, max._1, max._2))
      max
    }

  val stdevMagnitude = accelerometer
    .map(_.magnitude)
    .slider(25)
    .map(SeqMath.stdev(_))

  object MotionState extends Enumeration{
    type MotionState = Value
    val Walking, Queueing = Value

  }

  val motionState = autoCorrelation
    .combineLatest(stdevMagnitude)
    .scan(MotionState.Walking){case (oldState, ((tau, psi, _), stdev)) =>
      if(psi > 0.7){
        MotionState.Walking
      } else if(stdev < 0.5){
        MotionState.Queueing
      } else{
        oldState
      }
    }.distinctUntilChanged

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_motion_model)
    plot = findViewById(R.id.plot).asInstanceOf[XYPlot]
    plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED)
    series = new SimpleXYSeries("AutoCorrelation")
    plot.addSeries(series, new LineAndPointFormatter())
  }


  override def onResume(): Unit = {
    super.onResume()

    autoCorrelation
      .slider(20)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning { seq =>
        series.setModel(seq.map(_._2.asInstanceOf[java.lang.Double]).asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      }

    val textStepInterval = findViewById(R.id.textStepInterval).asInstanceOf[TextView]
    autoCorrelation
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{
        x => textStepInterval.setText("%d tau".format(x._1))
      }

    val textStdevAcc = findViewById(R.id.textStdevAcc).asInstanceOf[TextView]
    autoCorrelation
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ x =>
        textStdevAcc.setText("%.3f".format(x._3))
      }

    val textState = findViewById(R.id.textState).asInstanceOf[TextView]
    motionState
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ state =>
        textState.setText(state.toString)
      }

    val textSamplingRate = findViewById(R.id.textSamplingRate).asInstanceOf[TextView]
    accelerometer
      .observeOn(ExecutionContextScheduler(global))
      .map(_ => System.currentTimeMillis())
      .zipWithPrevious
      .map(t => (t._2 - t._1))
      .slidingBuffer(25, 25)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ diff =>
        val hertz = 1000 / diff.mean
        textSamplingRate.setText("%.1fHz".format(hertz))
      }
    
    val textState = findViewById(R.id.textState).asInstanceOf[TextView]
    val chiThres = 0.7
    val stdevThres = 0.5
    autoCorrelation
      .filter{case (tau, chi, stdev) => chi > chiThres || stdev < stdevThres}
      .map{case (tau, chi, stdev) => if (stdev < stdevThres) "Queueing" else "Walking"}
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ x =>
        textState.setText(x)
      }


    /////Learned metrics part
    val btnStartStop = findViewById(R.id.btn_start_stop).asInstanceOf[Button]
    val btnWalking = findViewById(R.id.btn_walking).asInstanceOf[Button]
    val btnQueueing = findViewById(R.id.btn_queueing).asInstanceOf[Button]
    //Add onclick listeners
    val startStopObs = Observable((aSubscriber: Subscriber[String]) => {
      btnStartStop.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext("Start")
      })
    }).scan((old: String, _: String) => if(old == "Start") "Stop" else "Start")
    .doOnEach(state => if(state == "Stop") btnStartStop.setText("Start") else btnStartStop.setText("Stop"))

    val walkingObs = Observable((aSubscriber: Subscriber[String]) => {
      btnWalking.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext("Walking")
      })
    })
    val queueingObs = Observable((aSubscriber: Subscriber[String]) => {
      btnQueueing.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext("Queueing")
      })
    })

    //TODO calculate times and stuff
    var testingTimer = System.currentTimeMillis()
    startStopObs
      .merge(walkingObs)
      .merge(queueingObs)
      .subscribe(state => {


    })

  }
}