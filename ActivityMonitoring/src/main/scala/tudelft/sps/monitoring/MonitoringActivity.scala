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
import android.widget.{ListView, TextView}
import tudelft.sps.lib.widget.FunctionalListAdapter
import scala.concurrent.duration._
import tudelft.sps.observable.{ObservingListAdapter, UIThreadScheduler, ObservableAccelerometer}

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
      .filter(f => !f.isNaN)
      .map(f => f: java.lang.Float)
      .slidingBuffer(20, 1)
      .observeOn(UIThreadScheduler(this))
      .subscribe((b) => {
        series1.setModel(b.asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      })

    val adapter = ObservingListAdapter[(String, String), (TextView, TextView)](getApplicationContext(), R.layout.signal_item){
      //for efficiency: findViewById is very expensive if it has to be done for each element, so these references are cached with this method
      view => (view.findViewById(R.id.mac).asInstanceOf[TextView], view.findViewById(R.id.rssi).asInstanceOf[TextView])
    }{ (holder, element) => //updates the view for the current element, using the viewholder
      holder._1.setText(element._1)
      holder._2.setText(element._2)
    }

    accelerometerSum
      .map(value => List(("value is ", value.toString())))
      .observeOn(UIThreadScheduler(this))
      .subscribe(adapter.onNext)

    findViewById(R.id.signals).asInstanceOf[ListView].setAdapter(adapter)
  }

}