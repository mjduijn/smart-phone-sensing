package tudelft.sps.monitoring

import android.app.Activity
import android.os.Bundle
import android.widget.{ListView, TextView}
import tudelft.sps.lib.widget.FunctionalListAdapter
import scala.concurrent.duration._
import tudelft.sps.observable.{UIThreadScheduler, ObservableAccelerometer}

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

    //values that are being displayed
    val seq = scala.collection.mutable.IndexedSeq[(String, String)]((for(i <- 1 to 1000) yield ("MAC" + i, "RSSI" + i)):_*)

    val adapter = FunctionalListAdapter[(String, String), (TextView, TextView)](seq, getApplicationContext(), R.layout.signal_item){
      //for efficiency: findViewById is very expensive if it has to be done for each element, so these references are cached with this method
      view => (view.findViewById(R.id.mac).asInstanceOf[TextView], view.findViewById(R.id.rssi).asInstanceOf[TextView])
    }{ (holder, element) => //updates the view for the current element, using the viewholder
      holder._1.setText(element._1)
      holder._2.setText(element._2)
    }

    findViewById(R.id.signals).asInstanceOf[ListView].setAdapter(adapter)
  }
}
