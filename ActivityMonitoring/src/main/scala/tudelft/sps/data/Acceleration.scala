package tudelft.sps.data

case class Acceleration(x: Float, y: Float, z: Float){
  def distance(that: Acceleration) = {
    Math.sqrt(Math.pow(x, that.x) + Math.pow(y, that.y) + Math.pow(z, that.z)).toFloat
  }
}