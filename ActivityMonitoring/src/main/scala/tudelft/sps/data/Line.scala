package tudelft.sps.data

case class Line(x0: Int,x1: Int, y0: Int, y1: Int) {

}

object Line {
  def nrOfWalls = 20

  def create9th(): Array[Line] = {
    val result = new Array[Line](nrOfWalls)
    result(0) = //Central hallway
  }
}