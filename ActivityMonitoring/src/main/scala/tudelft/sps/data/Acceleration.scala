package tudelft.sps.data

import android.hardware.SensorEvent

case class Acceleration(x: Float, y: Float, z: Float){
  def distance(that: Acceleration) = {
    Math.sqrt(Math.pow(Math.abs(x) - Math.abs(that.x), 2) + Math.pow(Math.abs(y) - Math.abs(that.x), 2) + Math.pow(Math.abs(z) - Math.abs(that.z), 2)).toFloat
  }

  def +(that: Acceleration) = Acceleration(this.x + that.x, this.y + that.y, this.z + that.z)
  def *(that: Acceleration) = Acceleration(this.x * that.x, this.y * that.y, this.z * that.z)
  def unary_- = Acceleration(-this.x, -this.y, - this.z)
  def -(that: Acceleration) = this + - that
  def /(c: Int) = Acceleration(this.x / c, this.y / c, this.z / c)

  def magnitude = distance(Acceleration.empty)
}

object Acceleration{
  def empty = Acceleration(0,0,0)
  def apply(event:SensorEvent):Acceleration = Acceleration(event.values(0), event.values(1), event.values(2))
}