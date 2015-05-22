package tudelft.sps.data



case class Line(x0: Int, x1: Int, y0: Int, y1: Int) {

}

object Line {
  val nrOfWalls = 20
  val scaling = 1000

  def toCo(c: Double):Int = (c * scaling).toInt

  def apply(x0: Double, x1: Double, y0: Double, y1: Double):Line =
    Line(toCo(x0), toCo(x1), toCo(y0), toCo(y1))

  def create9th(): Array[Line] = {
    val result = scala.collection.mutable.MutableList[Line]()//new Array[Line](nrOfWalls)


    //Room 1
    result += Line(0.0, 0, 0, 6.1)
    result += Line(0.0, 8, 0, 0)
    result += Line(8.0, 8, 0, 6.1)

    //Room 2
    result += Line(12.0, 12, 0, 6.1)
    result += Line(12.0, 16, 0, 0)
    result += Line(16.0, 16.0, 0, 6.1)

    //Room 3
    result += Line(16.0, 20, 0, 0)
    result += Line(20.0, 20, 0, 6.1)

    //Room 4
//    result += Line(12.0, 12, 11.3, 14.3)
    result += Line(12.0, 12, 8.2, 14.3)
    result += Line(12.0, 16, 14.3, 14.3)
//    result += Line(16.0, 16, 11.3, 14.3)
    result += Line(16.0, 16, 8.2, 14.3)

    //Room 5
    result += Line(56.0, 56.0, 8.2, 14.3)
    result += Line(56.0, 60.0, 14.3, 14.3)
    result += Line(60.0, 60.0, 8.2, 14.3)

    //Room 6
    result += Line(60.0, 64.0, 14.3, 14.3)
    result += Line(64.0, 64.0, 8.2, 14.3)

    //Central hallway
    result += Line(8.0, 12.0, 6.1, 6.1) //Upper
    result += Line(20.0, 72.0, 6.1, 6.1)

    result += Line(0, 0, 6.1, 8.1) //Vertical
    result += Line(72, 72, 6.1, 8.1)

    result += Line(0.0, 12.0, 8.2, 8.2) //Lower
    result += Line(16.0, 56.0, 8.2, 8.2)
    result += Line(64.0, 72.0, 8.2, 8.2)

    //TODO elevator??

    return result.toArray


  }


}


