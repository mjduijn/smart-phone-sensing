package tudelft.sps.observable

import android.app.Activity
import android.os.{PersistableBundle, Bundle}
import tudelft.sps.data.MotionState.MotionState
import tudelft.sps.data.{MotionModelDatabase, MotionState, KnnData}
import tudelft.sps.data.MotionState.MotionState
import tudelft.sps.lib.db.CursorSelector
import tudelft.sps.statistics.SeqExtensions.SeqMath
import tudelft.sps.statistics.SeqExtensions._
import tudelft.sps.statistics.{Knn, Classifier}
import tudelft.sps.lib.db._

trait ObservableMotionState extends Activity{
  this: Activity
    with ObservableAccelerometer
    with MotionModelDatabase =>

  private var _motionStateClassifier: Classifier[KnnData, MotionState] = null
  def motionStateClassifier = _motionStateClassifier


  private var maxStdev: Double = 1
  private var minStdev: Double = 0
  private var maxAvg: Double = 1
  private var minAvg: Double = 0

  val motionState = magnitudes
    .map{values =>
    val stdev = (values.stdev - minStdev) / (maxStdev - minStdev)
    val alpha = (SeqMath.alphaTrimmer(values, 0.1) - minAvg) / (maxAvg - minAvg)
    motionStateClassifier.classify(KnnData(stdev, alpha))
    }
    .slidingBuffer(3, 1)
    .map{ x =>
      if(x.count(_.equals(MotionState.Walking)) > 1) MotionState.Walking else MotionState.Queueing
    }.distinctUntilChanged

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val mapping: CursorSelector => KnnData = c => KnnData(c.getDouble("stdev"), c.getDouble("alpha"))

    val data = motionModelDatabase.getReadableDatabase(){ db =>
      val walks = db.mapped(mapping)(s"SELECT * FROM $walkTable", null)
        .map((_, MotionState.Walking))
      val queues = db.mapped(mapping)(s"SELECT * FROM $queueTable", null)
        .map((_, MotionState.Queueing))
      (KnnData.empty, MotionState.Queueing) :: (walks.merge(queues).toBlocking.toList)
    }

    maxStdev = data.maxBy(_._1.stdev)._1.stdev
    minStdev = data.minBy(_._1.stdev)._1.stdev
    maxAvg = data.maxBy(_._1.avg)._1.avg
    minAvg = data.minBy(_._1.avg)._1.avg

    val normalized_data = data.map{case (data, state) =>
      (KnnData((data.stdev - minStdev) / (maxStdev - minStdev), (data.avg - minAvg) / (maxAvg - minAvg)), state) 
    }

    _motionStateClassifier = Knn.traversableToKnn(normalized_data).toKnn(5, (a, b) => a.distance(b))
  }
}

