package tudelft.sps.observable

import android.app.Activity
import rx.lang.scala.{Subscriber, Observable, Subscription, Observer}

import scala.collection.mutable.ArrayBuffer

trait ManagedSubscriptions extends Activity{
  private val managedSubscriptions = ArrayBuffer[Subscription]()

  abstract override def onPause(): Unit = {
    super.onPause()
    managedSubscriptions.foreach(_.unsubscribe())
    managedSubscriptions.clear()
  }

  implicit class ManagedSubscriptionsObservableExtensions[A](obs:Observable[A]){
    def subscribeManaged(onNext:A => Unit): Unit = {
      managedSubscriptions += obs.subscribe(onNext)
    }

    def subscribeManaged(onNext:A => Unit, onError: Throwable => Unit): Unit = {
      managedSubscriptions += obs.subscribe(onNext, onError)
    }

    def subscribeManaged(onNext:A => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Unit = {
      managedSubscriptions += obs.subscribe(onNext, onError, onCompleted)
    }

    def subscribeManaged(observer:Observer[A]): Unit = {
      managedSubscriptions += obs.subscribe(observer)
    }

    def subscribeManaged(subscriber:Subscriber[A]): Unit = {
      managedSubscriptions += obs.subscribe(subscriber)
    }

    def subscribeManaged(): Unit = {
      managedSubscriptions += obs.subscribe()
    }
  }
}