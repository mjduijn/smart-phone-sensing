package tudelft.sps.monitoring

import android.app.Activity
import android.os.Bundle
import android.util.Log

class MonitoringActivity extends Activity with ObservableAccelerometer {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_hello)
    sensorChangedObservable.subscribe(x => Log.d("sensor", "sensor change: (%.1f,%.1f,%.1f)".format(x.values(0), x.values(1), x.values(2))))
  }
}
