package tudelft.sps.data

case class Line(x0: Int,x1: Int, y0: Int, y1: Int) {
  def doBoundingBoxesIntersect(x2:Int, x3:Int, y2:Int, y3:Int): Boolean = {
    var a0 = x0
    var a1 = x1
    var b0 = y0
    var b1 = y1
    if(a0 > a1){
      a0 = x1
      a1 = x0
    }
    if(b0 > y1){
      b0 = y1
      b1 = y0
    }
    var a2 = x2
    var a3 = x3
    var b2 = y2
    var b3 = y3
    if(a2 > a3){
      a2 = x3
      a3 = x2
    }
    if(b2 > y3){
      b2 = y3
      b3 = y2
    }
    a0 <= a3 &&
    a1 >= a2 &&
    b0 <= b3 &&
    b1 >= b2
  }

  def isPointOnLine(x:Int, y:Int): Boolean = {
    val dx = x1 - x0
    val dy = y1 - y0
    val cross = dx * (y - y0) - (x - x0) * dy
    Math.abs(cross) == 0
  }

  def isPointRightOfLine(x:Int, y:Int): Boolean = {
    val dx = x1 - x0
    val dy = y1 - y0
    val cross = dx * (y - y0) - (x - x0) * dy
    cross < 0
  }

  def lineSegmentTouchesOrCrossesLine(x2:Int, x3:Int, y2:Int, y3:Int) : Boolean =
    isPointOnLine(x2, y2) ||
    isPointOnLine(x3, y3) ||
    isPointRightOfLine(x2, y2) ^ isPointRightOfLine(x3, y3)

  def doLinesIntersect(x2:Int, x3:Int, y2:Int, y3:Int): Boolean =
    doBoundingBoxesIntersect(x2, x3, y2, y3) &&
    lineSegmentTouchesOrCrossesLine(x2, x3, y2, y3) &&
    lineSegmentTouchesOrCrossesLine(y2, y3, x2, x3)
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