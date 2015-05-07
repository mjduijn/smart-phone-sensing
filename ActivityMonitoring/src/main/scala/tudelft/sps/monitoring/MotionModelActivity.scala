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
import tudelft.sps.data.Acceleration
import tudelft.sps.observable.{ManagedSubscriptions, ObservableAccelerometer}
import tudelft.sps.statistics.SeqExtensions._
import tudelft.sps.observable._

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
    .map(Acceleration.apply)
    .slidingBuffer(tMax * 2, 10)
    .throttleLast(500 millis)
    .map{ sample =>
      val t0 = System.currentTimeMillis()
      def psi(m:Int) = {
        val magnitude = sample.map(_.magnitude)
        for (t_i <- tMin to (tMax - 1)) yield {
          val mean0 = magnitude.drop(m).take(t_i).mean
          val mean1 = magnitude.drop(m + t_i).take(t_i).mean
          val seq = for (k <- 0 to t_i - 1) yield
            (magnitude(m + k) - mean0) * (magnitude(m + k + t_i) - mean1)
          seq.sum / (t_i * magnitude.drop(m).take(t_i).variance * magnitude.drop(m + t_i).take(t_i).variance)
        }
      }
      val result = psi(0).max
      val dt = System.currentTimeMillis() - t0
      Log.d(TAG, "[%dms]autoCorrelation result: %.2f".format(dt, psi(0).max))
      result
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
    /*
    autoCorrelation
      .slider(20)
      //.observeOn(UIThreadScheduler(this))
      .subscribeRunning { seq =>
        val withIndex = seq.zipWithIndex.flatMap(x => Seq(x._2, x._1))
        series.setModel(withIndex.map(_.asInstanceOf[java.lang.Double]).asJava, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED)
        plot.redraw()
      }
    */
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