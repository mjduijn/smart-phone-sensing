package tudelft.sps.observable

import android.app.Activity
import android.content.Context
import android.hardware.{Sensor, SensorEvent, SensorEventListener, SensorManager}
import android.os.Bundle
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

trait ObservableAccelerometer extends Activity with SensorEventListener{

  private val sensorChangedSubject = PublishSubject[SensorEvent]()
  val accelerometer:Observable[SensorEvent] = sensorChangedSubject

  private var sensorManager:SensorManager = null
  private var accelerometerSensor:Sensor = null

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    this.sensorManager = getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]
    this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  }

  abstract override def onResume(): Unit = {
    super.onResume()
    sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
  }

  abstract override def onPause(): Unit = {
    super.onPause()
    sensorManager.unregisterListener(this)
  }

  override def onSensorChanged(event: SensorEvent): Unit = {
    sensorChangedSubject.onNext(event)
  }

  override def onAccuracyChanged(sensor: Sensor, acc: Int): Unit = {
  }
}