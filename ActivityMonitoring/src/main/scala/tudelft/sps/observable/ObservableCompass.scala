package tudelft.sps.observable

import android.app.Activity
import android.content.Context
import android.hardware.{Sensor, SensorEvent, SensorEventListener, SensorManager}
import android.os.Bundle
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

trait ObservableCompass extends Activity{

  private val sensorChangedSubject = PublishSubject[Double]()
  val compass:Observable[Double] = sensorChangedSubject.onBackpressureDrop

  private var sensorManager:SensorManager = null
  private var dep:Sensor = null

  private val compassEventListener = new CompassSensorEventListener()

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    this.sensorManager = getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]
    this.dep = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
  }

  abstract override def onResume(): Unit = {
    super.onResume()
    sensorManager.registerListener(compassEventListener, dep, SensorManager.SENSOR_DELAY_NORMAL)
  }

  abstract override def onPause(): Unit = {
    super.onPause()
    sensorManager.unregisterListener(compassEventListener)
  }

  private class CompassSensorEventListener extends SensorEventListener {

    override def onSensorChanged(event: SensorEvent): Unit = {
      if(event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
        val radians = Math.toRadians(event.values(0)) - Math.PI
        sensorChangedSubject.onNext(radians)
      }
    }

    override def onAccuracyChanged(p1: Sensor, p2: Int): Unit = {}
  }
}