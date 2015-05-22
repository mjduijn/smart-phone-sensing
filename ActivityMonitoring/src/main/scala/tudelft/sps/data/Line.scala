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

}