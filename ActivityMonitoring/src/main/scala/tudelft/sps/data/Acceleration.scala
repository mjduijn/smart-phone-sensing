package tudelft.sps.data

case class Acceleration(x: Float, y: Float, z: Float){
  def distance(that: Acceleration) = {
    Math.sqrt(Math.pow(Math.abs(x) - Math.abs(that.x), 2) + Math.pow(Math.abs(y) - Math.abs(that.x), 2) + Math.pow(Math.abs(z) - Math.abs(that.z), 2)).toFloat
  }

  def magnitude = distance(Acceleration.empty)
}

object Acceleration{
  def empty = Acceleration(0,0,0)
}