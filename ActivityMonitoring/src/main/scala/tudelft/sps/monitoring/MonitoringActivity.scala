package tudelft.sps.monitoring

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import tudelft.sps.observable.{UIThreadScheduler, ObservableAccelerometer}

class MonitoringActivity extends Activity with ObservableAccelerometer {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_hello)

    accelerometer
      .map(event => (event.values(0), event.values(1), event.values(2)))
      .observeOn(UIThreadScheduler(this))
      .subscribe{x =>
        findViewById(R.id.accelerometer_value).asInstanceOf[TextView].setText("sensor change: (%.1f,%.1f,%.1f)".format(x._1, x._2, x._3))
      }
  }
}
