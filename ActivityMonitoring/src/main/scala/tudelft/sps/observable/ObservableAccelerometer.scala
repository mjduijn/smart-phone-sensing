package tudelft.sps.observable

import android.app.Activity
import android.content.Context
import android.hardware.{Sensor, SensorEvent, SensorEventListener, SensorManager}
import android.os.Bundle
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.subjects.PublishSubject
import tudelft.sps.data._

import scala.concurrent.ExecutionContext.Implicits._

trait ObservableAccelerometer extends Activity{

  private val sensorChangedSubject = PublishSubject[SensorEvent]()
  val accelerometer:Observable[SensorEvent] = sensorChangedSubject.onBackpressureDrop

  val magnitudes = accelerometer
    .observeOn(ExecutionContextScheduler(global))
    .map(_.magnitude)
    .slidingBuffer(20, 5)

  private var sensorManager:SensorManager = null
  private var accelerometerSensor:Sensor = null
  private val accelerometerEventListener = new AccelerometerSensorEventListener()

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    this.sensorManager = getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]
    this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  }

  abstract override def onResume(): Unit = {
    super.onResume()
    sensorManager.registerListener(accelerometerEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
  }

  abstract override def onPause(): Unit = {
    super.onPause()
    sensorManager.unregisterListener(accelerometerEventListener)
  }

  private class AccelerometerSensorEventListener extends SensorEventListener{
    override def onSensorChanged(event: SensorEvent): Unit = {
      sensorChangedSubject.onNext(event)
    }

    override def onAccuracyChanged(sensor: Sensor, acc: Int): Unit = {
    }
  }
}