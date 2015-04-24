package tudelft.sps.observable

import rx.lang.scala.Scheduler
import rx.lang.scala.Subscription
import rx.functions.Action0
import rx.lang.scala.JavaConversions.javaSchedulerToScalaScheduler
import scala.concurrent.duration._
import android.app.Activity

object UIThreadScheduler {

  //@implicitNotFound("Could not find an Activity")
  def apply(activity:Activity): Scheduler = javaSchedulerToScalaScheduler(new rx.Scheduler {

    implicit def actionToRunnable(action: => Unit): Runnable = new Runnable { override def run(): Unit = action }

    override def createWorker(): rx.Scheduler.Worker = new rx.Scheduler.Worker() {

      val subscription = Subscription{}
      override def unsubscribe() = subscription.unsubscribe()
      override def isUnsubscribed = subscription.isUnsubscribed

      override def schedule(action: Action0): rx.Subscription = {
        activity.runOnUiThread{
          if(!isUnsubscribed) action.call()
        }
        this
      }

      override def schedule(action: Action0, delayTime: Long, unit: TimeUnit): rx.Subscription = {
        activity.runOnUiThread{
          Thread.sleep(Duration(delayTime, unit).toMillis)
          if(!isUnsubscribed) action.call()
        }
        this
      }
    }
  })
}