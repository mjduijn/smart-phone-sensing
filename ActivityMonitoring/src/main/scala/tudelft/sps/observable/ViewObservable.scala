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
      Log.d("MotionModelActivity", "subscribed to view onClick")
      val onClickListener = new OnClickListener {
        override def onClick(v: View): Unit = {
          obs.onNext(v)
          Log.d("MotionModelActivity", "ONCLICK");
        }
      }
      view.setOnClickListener(onClickListener)
      Subscription{
        Log.d("MotionModelActivity", "unsubcribed")
        view.setOnClickListener(null)
      }
    }
  }
}