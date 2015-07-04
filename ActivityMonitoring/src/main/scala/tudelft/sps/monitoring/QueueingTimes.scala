package tudelft.sps.monitoring

import android.app.Activity
import android.util.Log
import rx.lang.scala.{Subscription, Observable, Subject}
import rx.lang.scala.subjects.PublishSubject
import tudelft.sps.data.{MotionState, KnnData}
import tudelft.sps.observable.{ManagedSubscriptions, ObservableMotionState}
import tudelft.sps.statistics.SeqExtensions.{StatisticalSeqExtensions, SeqMath}

import scala.collection.mutable.{ListBuffer, ArrayBuffer}

trait QueueingTimes extends Activity {
  this: Activity
    with ObservableMotionState
    with ManagedSubscriptions =>

  private val queueingTimeSubject = PublishSubject[QueueTimeData]()
  val queueingTime = queueingTimeSubject.asInstanceOf[Observable[QueueTimeData]]

  private var queueingSubscription:Subscription = null

  def startQueueingMeasurement(): Unit ={

    if(queueingSubscription != null && !queueingSubscription.isUnsubscribed){
      queueingSubscription.unsubscribe()
    }

    var time = System.currentTimeMillis()
    val episodes = ArrayBuffer[Long]()
    val threshold = 5000

    queueingSubscription = motionState.subscribe{ _ match {
      case MotionState.Queueing => {
        if(System.currentTimeMillis() - time > threshold){
          queueingTimeSubject.onNext(QueueTimeData(System.currentTimeMillis() - episodes.head, episodes.toList))
          queueingSubscription.unsubscribe()
        } else{
          episodes += System.currentTimeMillis()
          time = System.currentTimeMillis()
        }
      }
      case MotionState.Walking if episodes.nonEmpty => {
        episodes += System.currentTimeMillis()
        time = System.currentTimeMillis()
      }
    }}
  }

  case class QueueTimeData(total:Long, serviceTimes:List[Long]){
    lazy val groupedServiceTimes = serviceTimes.grouped(2).toList.collect{case x :: y :: _ => (y - x)}.toList
    def stdev = groupedServiceTimes.stdev
    def average = groupedServiceTimes.mean
  }

}