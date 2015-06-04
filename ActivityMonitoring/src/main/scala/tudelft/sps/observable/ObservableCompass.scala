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
//  private var compassSensor:Sensor = null
//  private var accelerometer:Sensor = null
  private var dep:Sensor = null

  private val compassEventListener = new CompassSensorEventListener()

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    this.sensorManager = getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]
//    this.compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
//    this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    this.dep = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
  }

  abstract override def onResume(): Unit = {
    super.onResume()
//    sensorManager.registerListener(compassEventListener, compassSensor, SensorManager.SENSOR_DELAY_GAME)
//    sensorManager.registerListener(compassEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

    sensorManager.registerListener(compassEventListener, dep, SensorManager.SENSOR_DELAY_GAME)
  }

  abstract override def onPause(): Unit = {
    super.onPause()
    sensorManager.unregisterListener(compassEventListener)
  }

  private class CompassSensorEventListener extends SensorEventListener{
    private var gravity: Array[Float] = null
    private var geomagnetic: Array[Float] = null

    override def onSensorChanged(event: SensorEvent): Unit = {
      if(event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
        val degree = event.values(0)
        val radians = Math.toRadians(degree) - Math.PI
        sensorChangedSubject.onNext(radians)
      }

//      if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//        gravity = event.values
//      } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
//        geomagnetic = event.values
//      }
//      if (gravity != null && geomagnetic != null) {
//        val r = new Array[Float](9)
//        val i = new Array[Float](9)
//        val success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)
//        if (success) {
//          val orientation = new Array[Float](3)
//          SensorManager.getOrientation(r, orientation)
//          sensorChangedSubject.onNext(orientation(0))
//        }
//      }
    }



    override def onAccuracyChanged(p1: Sensor, p2: Int): Unit = {}
  }
}