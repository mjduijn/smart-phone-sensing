package tudelft.sps.monitoring

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
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
      .map(sum => if(sum > 15) "Walking" else "Queueing")
      .observeOn(UIThreadScheduler(this))
      .subscribe{guess =>
        findViewById(R.id.activity_guess).asInstanceOf[TextView].setText(guess)
      }
  }
}
