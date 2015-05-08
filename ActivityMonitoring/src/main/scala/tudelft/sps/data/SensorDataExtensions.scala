package tudelft.sps

import android.hardware.SensorEvent

package object data{
  implicit class SensorEventExtensions(val sensorData:SensorEvent){
    def magnitude = Math.sqrt(Math.pow(sensorData.values(0), 2) + Math.pow(sensorData.values(1), 2) + Math.pow(sensorData.values(2), 2))
  }
}