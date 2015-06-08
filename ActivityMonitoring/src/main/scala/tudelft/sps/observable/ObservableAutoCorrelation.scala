package tudelft.sps.observable

import android.app.Activity
import rx.lang.scala.schedulers.ExecutionContextScheduler
import tudelft.sps.statistics.SeqExtensions.SeqMath
import tudelft.sps.data._

import scala.concurrent.ExecutionContext.Implicits._

trait ObservableAutoCorrelation extends Activity{ this: Activity with ObservableAccelerometer =>
  val tMin = 20
  val tMax = 50

  val autoCorrelation = accelerometer
    .observeOn(ExecutionContextScheduler(global))
    .map(_.magnitude)
    .slidingBuffer(tMax * 2, 5)
    .map{ sample =>
    var t_i = tMin
    var max: (Int, Double, Double) = (0, 0, 0)
    while (t_i <= tMax) {
      val m = tMax * 2 - 2 * t_i
      val mean0 = SeqMath.mean(sample, m, m + t_i)
      val mean1 = SeqMath.mean(sample, m + t_i, m + t_i * 2)

      var k = 0
      var sum:Double = 0
      while(k < t_i){
        sum = sum + (sample(m + k) - mean0) * (sample(m + k + t_i) - mean1)
        k = k + 1
      }
      val stdev1 = SeqMath.stdev(sample, m + t_i, m + t_i * 2)
      val chi = sum / (t_i * SeqMath.stdev(sample, m, m + t_i) * stdev1)
      if(chi > max._2){
        max = (t_i, chi, stdev1)
      }
      t_i = t_i + 1
    }
    max
  }

  val tau = ((30, 1d, 0d) +: autoCorrelation)
    .observeOn(ExecutionContextScheduler(global))
    .filter(_._2 > 0.7)
    .map(_._1)
    .slider(20)
    .map(x => SeqMath.alphaTrimmer(x.map(_.toDouble), 0.2))

}