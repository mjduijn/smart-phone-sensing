package tudelft.sps.plot

import scala.collection.mutable.ArrayBuffer
/*
TODO This can probably be removed, this was for the not-working graph library

trait BufferedXYDataSource extends XYSeries{
  private var buffer = ArrayBuffer[(Number, Number)]()

  def bufferSize:Int

  override def size(): Int = buffer.length

  override def getY(index: Int): Number = buffer(index)._2

  override def getX(index: Int): Number = buffer(index)._1

  def push(value:(Number, Number)):Unit = {
    buffer += value
    if(buffer.length > bufferSize){
      buffer = buffer.tail
    }
  }
}

object BufferedXYDataSource{
  def apply(size:Int, title:String):BufferedXYDataSource = new BufferedXYDataSource {
    override def bufferSize: Int = size
    override def getTitle: String = title
  }
}
*/