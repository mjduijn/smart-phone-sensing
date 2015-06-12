package tudelft.sps.monitoring

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import android.app.Activity
import android.content.Intent
import android.graphics._
import android.os.{PersistableBundle, Bundle}
import android.preference.PreferenceManager
import android.text.format.Time
import android.util.Log
import android.view.{Menu, WindowManager, View}
import android.view.View.OnClickListener
import android.widget.ImageView.ScaleType
import android.widget.{ImageView, Button, TextView}
import com.androidplot.xy.{BoundaryMode, LineAndPointFormatter, XYPlot, SimpleXYSeries}
import tudelft.sps.data.MotionState.MotionState
import tudelft.sps.lib.widget.FloorMapView
import tudelft.sps.statistics.{Classifier, Knn}
import tudelft.sps.data.MotionState
import scala.concurrent.duration._
import rx.lang.scala.{Subscriber, Observable}
import rx.lang.scala.schedulers.ExecutionContextScheduler
import tudelft.sps.observable.{ManagedSubscriptions, ObservableAccelerometer}
import tudelft.sps.statistics.SeqExtensions._
import tudelft.sps.observable._
import tudelft.sps.data._
import tudelft.sps.observable.ViewObservable._
import tudelft.sps.lib.db._

import scala.concurrent.ExecutionContext.Implicits._

class MotionModelActivity extends Activity
  with ObservableAccelerometer
  with ManagedSubscriptions
  with ObservableCompass
  with ObservableAutoCorrelation
  with MotionModelDatabase
  with ObservableMotionState
  with QueueingTimes
{

  val TAG = "MotionModelActivity"

  var plot:XYPlot = null
  var series:SimpleXYSeries = null

  val stdevMagnitude = accelerometer
    .map(_.magnitude)
    .slider(25)
    .map(SeqMath.stdev(_))


  override def onCreate(savedInstanceState: Bundle): Unit = {

    super.onCreate(savedInstanceState)
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_motion_model)

//    plot = findViewById(R.id.plot).asInstanceOf[XYPlot]
//    plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED)
//    series = new SimpleXYSeries("AutoCorrelation")
//    plot.addSeries(series, new LineAndPointFormatter())
  }

  override def onResume(): Unit = {
    super.onResume()

    val floormap = FloorMap(10000)

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)

    val textStepInterval = findViewById(R.id.textStepInterval).asInstanceOf[TextView]
    tau
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{x =>
        textStepInterval.setText("%.2f tau".format(x))
      }

    val textStdevAcc = findViewById(R.id.textStdevAcc).asInstanceOf[TextView]

    magnitudes
      .map(SeqMath.stdev(_))
      .observeOn(UIThreadScheduler(this))
      .subscribe(x => textStdevAcc.setText("%.3f".format(x)))

    val textAvgAcc = findViewById(R.id.textAvgAcc).asInstanceOf[TextView]
    magnitudes
      .map(SeqMath.mean(_))
      .observeOn(UIThreadScheduler(this))
      .subscribe(x => textAvgAcc.setText("%.3f".format(x)))

    val textAlphaTrimmerAcc = findViewById(R.id.textAlphaTrimAcc).asInstanceOf[TextView]
    magnitudes
      .map(SeqMath.alphaTrimmer(_, 0.1))
      .observeOn(UIThreadScheduler(this))
      .subscribe(x => textAlphaTrimmerAcc.setText("%.3f".format(x)))

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
    motionState
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning(x => textState.setText(x.toString))


    /////Learned metrics part
    val btnStartStop = findViewById(R.id.btn_start_stop).asInstanceOf[Button]
    val btnWalking = findViewById(R.id.btn_walking).asInstanceOf[Button]
    val btnQueueing = findViewById(R.id.btn_queueing).asInstanceOf[Button]
    //Add onclick listeners
    val startStopObs= btnStartStop.onClick
      .observeOn(ExecutionContextScheduler(global))
    .toggle()
    .doOnEach(if(_){
      btnStartStop.setText("Start")
    } else {
      btnStartStop.setText("Stop")
    })


    def train(obs:Observable[Boolean], table:String): Unit = obs
      .observeOn(ExecutionContextScheduler(global))
      .doOnEach(if(_){motionModelDatabase.getWritableDatabase().delete(table, null, null)})
      .combineLatestWith(magnitudes)((b, e) => (b, e))
      .filter(_._1)
      .map(_._2)
//      .slidingBuffer(50, 10)
      .subscribeRunning{ points =>
        val stdev = points.stdev
        val alpha = SeqMath.alphaTrimmer(points, 0.1)
        motionModelDatabase.getWritableDatabase(){ db =>
          db.insertRow(table,
            "stdev" -> stdev,
            "alpha" -> alpha
          )
        }
      }

    val btnTrainWalking = findViewById(R.id.btn_train_walking).asInstanceOf[Button]
    train(btnTrainWalking.onClick.toggle(false, true).doOnEach(b => btnTrainWalking.setText(if(b) "Stop Training" else "Train Walking")), walkTable)
    val btnTrainQueueing = findViewById(R.id.btn_train_queueing).asInstanceOf[Button]
    train(btnTrainQueueing.onClick.toggle(false, true).doOnEach(b => btnTrainQueueing.setText(if(b) "Stop Training" else "Train Queueing")), queueTable)

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

    val iv = this.findViewById(R.id.image_floor_plan).asInstanceOf[FloorMapView]
    iv.floorMap = floormap

    case class MovementData(compass:Double, state:MotionState, tau:Double)

    val angleDiff = prefs.getString("angleOffset", "0").toDouble
    val strideLength = prefs.getString("strideLength", "700").toInt
    val redrawSpeed = prefs.getString("redrawSpeed", "500").toInt
    val movementData = compass
      .observeOn(ExecutionContextScheduler(global))
      .combineLatest(motionState)
      .combineLatest(tau)
      .sample(redrawSpeed millis)
      .map(x => MovementData(x._1._1, x._1._2, x._2))

    movementData
      .filter(_.state.equals(MotionState.Walking))
      .foreach { data =>
        Log.d(TAG, "new movement data: " + movementData)
        var angle = data.compass + angleDiff
        while (angle < -Math.PI) {
          angle += 2 * Math.PI
        }
        while (angle > Math.PI) {
          angle -= 2 * Math.PI
        }
        val distance = strideLength * (50 / data.tau) * redrawSpeed * 0.001
        floormap.move(distance.toInt, angle)
      }

      floormap.drawObs
      .observeOn(ExecutionContextScheduler(global))
      .subscribeRunning{ _ =>
        iv.redraw()
      }

    val btnSettings = findViewById(R.id.btn_settings).asInstanceOf[Button]
    btnSettings.onClick.subscribeRunning{ x =>
      val intent = new Intent(this, classOf[SettingsActivity])
      startActivity(intent)
    }

    val textCompass = findViewById(R.id.textCompass).asInstanceOf[TextView]
    compass.subscribeRunning{ x =>

      var angle = x + angleDiff
      while(angle < -Math.PI) {
        angle += 2 * Math.PI
      }
      while(angle > Math.PI) {
        angle -= 2 * Math.PI
      }

      textCompass.setText("%.2f".format(angle))
    }

    val btnQueueingTime = findViewById(R.id.btn_queueing_time).asInstanceOf[Button]
    btnQueueingTime.onClick.subscribeRunning{ x =>
      startQueueingMeasurement()
      btnQueueingTime.setText(R.string.resetQueueingTime)
    }

    val textQueuingTime = findViewById(R.id.textQueueingTime).asInstanceOf[TextView]
    queueingTime
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{x =>
      textQueuingTime.setText(s"${x.average}(${x.stdev}})")
      btnQueueing.setText(R.string.queueingTime)
    }

  }
}