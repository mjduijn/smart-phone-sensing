package tudelft.sps.statistics

import Numeric._
import scala.math._

trait Distance[DistanceType, Data] {
  def distance(d1 : Data, d2 : Data) : DistanceType
}

trait Cartesian1[Data] extends Distance[Data, Data] {
  val numeric : Numeric[Data]

  def distance(d1 : Data, d2 : Data) = numeric.abs(numeric.minus(d1, d2))
}

trait Cartesian2[Data] extends Distance[Data, (Data, Data)] {
  def distance(d1 : (Data,Data), d2 : (Data,Data))(implicit numeric: Numeric[Data]) =
    sqrt(numeric.toDouble(numeric.plus(numeric.minus(d1._1, d2._1), numeric.minus(d1._2, d2._2))))
}

trait CartesianN[Data] extends Distance[Data, Product] {
  def distance(d1 : Product, d2 : Product)(implicit numeric: Numeric[Data]) = {
    val diffs = for (
      i <- 0 until d1.productArity;
      e1 = d1.productElement(i).asInstanceOf[Data];
      e2 = d1.productElement(i).asInstanceOf[Data]
    ) yield numeric.minus(e1, e2)
    sqrt(numeric.toDouble(diffs.sum))
  }
}

/**
 * k-NN algorithm. It classify a sample, given a dataset of known elements
 *
 * <p>
 * It needs: a dataset and a function to compute the distance between two items
 * </p>
 */
abstract class Knn[Data, Label,
DistanceType <% Ordered[DistanceType]]
(
  val k : Int,
  val dataset : List[(Data, Label)]
  )
  extends Classifier[Data, Label] with Distance[DistanceType, Data]
{
  if(k < 1)
    throw new scala.IllegalArgumentException("k must be positive")

  def classify(sample : Data) : Label = {

    // Compute distances between sample and dataset items
    val distances = for(
      (d, label) <- dataset
    ) yield (d, label, distance(d, sample))

    // take first k items sorted by distance
    val firstk = distances sortBy ( d => d._3 ) take k

    // classification is given by the max number of occurrence of a label
    val classification = firstk groupBy (d => d._2) maxBy (g => g._2.length)

    classification._1
  }
}

object Knn {
  class KnnM[Data,Label] (data : List[(Data,Label)]) {
    def toKnn[DistanceType <% Ordered[DistanceType]](k : Int, distanceFunc : (Data, Data) => DistanceType) =
      new Knn[Data,Label,DistanceType] (k, data) {
        def distance(d1: Data, d2: Data) = distanceFunc(d1, d2)
      }
  }

  implicit def traversableToKnn[Data,Label](data : Traversable[(Data, Label)]) = new KnnM[Data,Label](data.toList)
}