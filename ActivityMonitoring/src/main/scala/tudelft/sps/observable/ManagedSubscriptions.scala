package tudelft.sps.observable

import android.app.Activity
import rx.lang.scala.{Subscriber, Observable, Subscription, Observer}

import scala.collection.mutable.ArrayBuffer

trait ManagedSubscriptions extends Activity{
  private val runningSubscriptions = ArrayBuffer[Subscription]()
  private val aliveSubscriptions = ArrayBuffer[Subscription]()

  abstract override def onPause(): Unit = {
    super.onPause()
    runningSubscriptions.foreach(_.unsubscribe())
    runningSubscriptions.clear()
  }

  abstract override def onStop(): Unit ={
    super.onStop()
    aliveSubscriptions.foreach(_.unsubscribe())
    aliveSubscriptions.clear()
  }

  implicit class ManagedSubscriptionsObservableExtensions[A](obs:Observable[A]){
    def subscribeRunning(onNext:A => Unit): Unit = {
      runningSubscriptions += obs.subscribe(onNext)
    }

    def subscribeRunning(onNext:A => Unit, onError: Throwable => Unit): Unit = {
      runningSubscriptions += obs.subscribe(onNext, onError)
    }

    def subscribeRunning(onNext:A => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Unit = {
      runningSubscriptions += obs.subscribe(onNext, onError, onCompleted)
    }

    def subscribeRunning(observer:Observer[A]): Unit = {
      runningSubscriptions += obs.subscribe(observer)
    }

    def subscribeRunning(subscriber:Subscriber[A]): Unit = {
      runningSubscriptions += obs.subscribe(subscriber)
    }

    def subscribeRunning(): Unit = {
      runningSubscriptions += obs.subscribe()
    }

    def subscribeAlive(onNext:A => Unit): Unit = {
      aliveSubscriptions += obs.subscribe(onNext)
    }

    def subscribeAlive(onNext:A => Unit, onError: Throwable => Unit): Unit = {
      aliveSubscriptions += obs.subscribe(onNext, onError)
    }

    def subscribeAlive(onNext:A => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Unit = {
      aliveSubscriptions += obs.subscribe(onNext, onError, onCompleted)
    }

    def subscribeAlive(observer:Observer[A]): Unit = {
      aliveSubscriptions += obs.subscribe(observer)
    }

    def subscribeAlive(subscriber:Subscriber[A]): Unit = {
      aliveSubscriptions += obs.subscribe(subscriber)
    }

    def subscribeAlive(): Unit = {
      aliveSubscriptions += obs.subscribe()
    }
  }
}