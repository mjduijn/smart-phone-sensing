package tudelft.sps.monitoring

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.androidplot.xy.{LineAndPointFormatter, SimpleXYSeries, XYSeries, XYPlot}
import rx.lang.scala.Observable
import scala.concurrent.duration._
import tudelft.sps.observable.{UIThreadScheduler, ObservableAccelerometer}
import scala.collection.JavaConverters._

class MonitoringActivity extends Activity with ObservableAccelerometer {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_hello)

    val accelerometerSum = accelerometer
      .slidingBuffer(500 millis, 0 millis)
      .map{buffer =>
        (
          buffer.map(event => Math.abs(event.values(0))).sum +
          buffer.map(event => Math.abs(event.values(1))).sum +
          buffer.map(event => Math.abs(event.values(2))).sum
        ) / buffer.length
      }

    accelerometerSum
      .observeOn(UIThreadScheduler(this))
      .subscribe{x =>
        findViewById(R.id.accelerometer_value).asInstanceOf[TextView].setText("Accelerometer: %.2f".format(x))
      }

    accelerometerSum
      .map(sum => if(sum > 3) "Walking" else "Queueing")
      .observeOn(UIThreadScheduler(this))
      .subscribe{guess =>
        findViewById(R.id.activity_guess).asInstanceOf[TextView].setText(guess)
      }

    val plot = findViewById(R.id.mySimpleXYPlot).asInstanceOf[XYPlot]
    val series1 = new SimpleXYSeries("Series 1")
    plot.addSeries(series1, new LineAndPointFormatter())

    accelerometerSum
      .map(i => i: java.lang.Float)
      .slidingBuffer(20, 1)
      .observeOn(UIThreadScheduler(this))
      .subscribe((b) => {
        series1.setModel(b.asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      })
  }

}
