package tudelft.sps.monitoring

import android.app.Activity
import android.util.Log
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.{Subscription, Observable, Subject}
import rx.lang.scala.subjects.PublishSubject
import tudelft.sps.data.{MotionState, KnnData}
import tudelft.sps.observable.{ManagedSubscriptions, ObservableMotionState}
import tudelft.sps.statistics.SeqExtensions.{StatisticalSeqExtensions, SeqMath}
import scala.concurrent.ExecutionContext.Implicits._

import scala.collection.mutable.{ListBuffer, ArrayBuffer}

trait QueueingTimes extends Activity {
  this: Activity
    with ObservableMotionState
    with ManagedSubscriptions =>

  private val queueingTimeSubject = PublishSubject[QueueTimeData]()
  val queueingTime = queueingTimeSubject.asInstanceOf[Observable[QueueTimeData]]

  private var queueingSubscription:Subscription = null

  def startQueueingMeasurement(): Unit ={
    Log.d("MotionModelActivity", "STARTING QUEUEING MEASUREMENT")


    if(queueingSubscription != null && !queueingSubscription.isUnsubscribed){
      queueingSubscription.unsubscribe()
    }

    var time = System.currentTimeMillis()
    val episodes = ArrayBuffer[Long]()
    val threshold = 5000

    queueingSubscription = motionState
      .observeOn(ExecutionContextScheduler(global))
      .subscribeRunning{ _ match {
      case MotionState.Queueing => {
        Log.d("MotionModelActivity", "QUEUEING")
        if(System.currentTimeMillis() - time > threshold){
          queueingTimeSubject.onNext(QueueTimeData(System.currentTimeMillis() - episodes.head, episodes.toList))
          queueingSubscription.unsubscribe()
        } else{
          episodes += System.currentTimeMillis()
          time = System.currentTimeMillis()
        }
      }
      case MotionState.Walking if episodes.nonEmpty => {
        Log.d("MotionModelActivity", "WALKING")
        episodes += System.currentTimeMillis()
        time = System.currentTimeMillis()
      }
      case other => Log.d("MotionModelActivity", "NOPE")
    }}
  }

  case class QueueTimeData(val total:Long, val serviceTimes:List[Long]){
    lazy val groupedServiceTimes = serviceTimes.grouped(2).toList.collect{case x :: y :: _ => (y - x)}.filter{_ > 0.5}.toList
    def stdev = groupedServiceTimes.stdev
    def average = groupedServiceTimes.mean
  }

}