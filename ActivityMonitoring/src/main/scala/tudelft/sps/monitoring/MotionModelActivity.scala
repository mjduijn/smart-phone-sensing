package tudelft.sps.monitoring

import android.app.Activity
import android.os.{PersistableBundle, Bundle}
import android.util.Log
import android.widget.TextView
import com.androidplot.xy.{BoundaryMode, LineAndPointFormatter, XYPlot, SimpleXYSeries}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import rx.lang.scala.Observable
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
  val tMax = 75

  val autoCorrelation = accelerometer
    .observeOn(ExecutionContextScheduler(global))
    .map(_.magnitude)
    .slidingBuffer(tMax * 2, 25)
    .throttleLast(500 millis)
    .map{ sample =>
      def psi(m_old:Int) = {
        val t0 = System.currentTimeMillis()

        var t_i = tMin
        var max: (Int, Double) = (0, 0)
        while (t_i < (tMax - 1)) {
          val m = tMax * 2 - 2 * t_i
          val mean0 = sample.drop(m).take(t_i).mean
          val mean1 = sample.drop(m + t_i).take(t_i).mean
          var k = 0
          var sum:Double = 0
          while(k < t_i - 1){
            sum = sum + (sample(m + k) - mean0) * (sample(m + k + t_i) - mean1)
            k = k + 1
          }
          val chi = sum / (t_i * sample.drop(m).take(t_i).stdev * sample.drop(m + t_i).take(t_i).stdev)
          if(chi > max._2){
            max = (t_i, chi)
          }
          t_i = t_i + 1
        }
        val dt = System.currentTimeMillis() - t0
        Log.d(TAG, "[%s][%dms]autoCorrelation result: (%d, %.2f)".format(Thread.currentThread().getName, dt, max._1, max._2))
        max
      }
      psi(0)
    }

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
    val textStepInterval = findViewById(R.id.textStepInterval).asInstanceOf[TextView]

    autoCorrelation
      .slider(20)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning { seq =>
        series.setModel(seq.map(_._2.asInstanceOf[java.lang.Double]).asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      }

    autoCorrelation
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{
        x => textStepInterval.setText("%d tau".format(x._1))
      }

    val textStdevAcc = findViewById(R.id.textStdevAcc).asInstanceOf[TextView]
    accelerometer
      .map(_.magnitude)
      .slider(100)
      .map(_.stdev)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ stdev =>
        textStdevAcc.setText("%.3f".format(stdev))
      }

    val textSamplingRate = findViewById(R.id.textSamplingRate).asInstanceOf[TextView]
    accelerometer
      .map(_ => System.currentTimeMillis())
      .zipWithPrevious
      .map(t => t._2 - t._1)
      .slidingBuffer(25, 25)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ diff =>
        val hertz = 1000 / diff.mean
        textSamplingRate.setText("%.1fHz".format(hertz))
      }
  }
}