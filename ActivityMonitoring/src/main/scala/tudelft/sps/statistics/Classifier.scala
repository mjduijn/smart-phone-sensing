package tudelft.sps.statistics

/**
 * User: onofrio.panzarino@gmail.com
 * Date: 24/07/11
 */

trait Classifier[Data, Label] {

  def classify(data : Data) : Label

}

trait SupervisedLearning[Data, Label, +TClassifier <: Classifier[Data, Label]] {

  def trainWith(trainingSet : Traversable[(Data, Label)]) : TClassifier

}