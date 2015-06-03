package tudelft.sps.observable

import android.telephony.SubscriptionInfo
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import rx.lang.scala.Observable
import rx.lang.scala.Subscription

object ViewObservable{

  implicit class ViewObservableExtensions(val view:View) extends AnyVal{
    def onClick:Observable[View] = Observable.create{ obs =>
      val onClickListener = new OnClickListener {
        override def onClick(v: View): Unit = {
          obs.onNext(v)
        }
      }
      view.setOnClickListener(onClickListener)
      Subscription{
        view.setOnClickListener(null)
      }
    }
  }
}