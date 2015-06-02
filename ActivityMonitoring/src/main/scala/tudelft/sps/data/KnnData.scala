package tudelft.sps.data

case class KnnData(stdev: Double, avg: Double){
  def distance(that: KnnData) = {
    Math.sqrt(Math.pow(stdev - that.stdev, 2) + Math.pow(avg - that.avg, 2))
  }
}

object KnnData{
  def empty = KnnData(0, 0)
}