package tudelft.sps.monitoring

import android.app.Activity
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.{Button, TextView, ListView}
import com.androidplot.xy.{LineAndPointFormatter, SimpleXYSeries, XYSeries, XYPlot}
import rx.lang.scala.{Subscriber, Observer, Observable}
import tudelft.sps.data.Acceleration
import scala.collection.mutable
import scala.concurrent.duration._
import tudelft.sps.observable._
import scala.collection.JavaConverters._
import android.widget.{ListView, TextView}
import rx.lang.scala.Observable
import tudelft.sps.wifi.{WifiSignal, ObservableWifiManager}
import scala.concurrent.duration._
import tudelft.sps.statistics.{Classifier, Knn}

class MonitoringActivity extends Activity
  with ObservableAccelerometer
  with ObservableWifiManager
  with ManagedSubscriptions
{
  var classifier: Classifier[Acceleration, Int] = null

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_hello)

    //TODO read database, get list and train below
    classifier = Knn.traversableToKnn(List((Acceleration(0, 0, 0), 0))).toKnn(1, (a, b) => a.distance(b))
  }

  override def onResume(): Unit = {
    super.onResume()

    val accelerometerSum = accelerometer
      .slidingBuffer(500 millis, 0 millis)
      .map{buffer =>
      (
        buffer.map(event => Math.abs(event.values(0))).sum +
          buffer.map(event => Math.abs(event.values(1))).sum +
          buffer.map(event => Math.abs(event.values(2))).sum
        ) / buffer.length
    }

    accelerometerSum
      .observeOn(UIThreadScheduler(this))
      .subscribeManaged{x =>
        findViewById(R.id.accelerometer_value).asInstanceOf[TextView].setText("Accelerometer: %.2f".format(x))
      }

//    accelerometerSum
//      .map(sum => if (sum > 3) "Walking" else "Queueing")
//      .observeOn(UIThreadScheduler(this))
//      .subscribeManaged{guess =>
//        findViewById(R.id.activity_guess).asInstanceOf[TextView].setText(guess)
//      }

    accelerometer
      .map(event => Acceleration(event.values(0), event.values(1), event.values(2)))
      .map(a => classifier.classify(a))
      .doOnEach(println(_))
      .map(v => if (v == 0) "Queueing" else "Walking" )
      .subscribeManaged{guess =>
        findViewById(R.id.activity_guess).asInstanceOf[TextView].setText(guess)
      }



    val plot = findViewById(R.id.mySimpleXYPlot).asInstanceOf[XYPlot]
    val series1 = new SimpleXYSeries("Series 1")
    plot.addSeries(series1, new LineAndPointFormatter())

    accelerometerSum
      .filter(f => !f.isNaN)
      .map(f => f: java.lang.Float)
      .slidingBuffer(20, 1)
      .observeOn(UIThreadScheduler(this))
      .subscribeManaged { b =>
        series1.setModel(b.asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      }

    val adapter = ObservingListAdapter[WifiSignal, (TextView, TextView)](getApplicationContext(), R.layout.signal_item){
      //for efficiency: findViewById is very expensive if it has to be done for each element, so these references are cached with this method
      view => (view.findViewById(R.id.mac).asInstanceOf[TextView], view.findViewById(R.id.rssi).asInstanceOf[TextView])
    }{ (holder, element) => //updates the view for the current element, using the viewholder
      holder._1.setText(element.mac)
      holder._2.setText(element.rssi.toString)
    }
    wifiScans
      .observeOn(UIThreadScheduler(this))
      .subscribeManaged(adapter.onNext)

    findViewById(R.id.signals).asInstanceOf[ListView].setAdapter(adapter)

    Observable.just(-1) //to prevent initial delay
      .merge(Observable.interval(1 second))
      .subscribeManaged(_ => startWifiscan())

    val btnLearnWalking = findViewById(R.id.btn_learn_walking).asInstanceOf[Button]

    //Add learn walking onclick listener
    val learnWalkingObservable = Observable((aSubscriber: Subscriber[Int]) => {
      btnLearnWalking.setOnClickListener(new OnClickListener {
          override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext(1)
        //TODO deal with unsubscribe
      })
    }).scan(false)((x, i) => !x)

    learnWalkingObservable
      .combineLatestWith(accelerometerSum)((b, s) => (b, s))
      .observeOn(UIThreadScheduler(this))
      .foreach{case (b, s) => {
        if(b) {
          btnLearnWalking.setText("Learn walking")
          //TODO enter s into db
        }
        else {
          btnLearnWalking.setText("Stop learn walking")
        }
      }}


    val btnLearnQueuing = findViewById(R.id.btn_learn_queuing).asInstanceOf[Button]
    //Add learn queuing onclick listener
    val learnQueuingObservable = Observable((aSubscriber: Subscriber[Int]) => {
      btnLearnQueuing.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext(1)
      })
    })
    .scan(false)((x, i) => !x)

    //Change button text
    learnQueuingObservable
      .combineLatestWith(accelerometerSum)((b, s) => (b, s))
      .observeOn(UIThreadScheduler(this))
      .foreach{case (b, s) => {
        if(b) {
          btnLearnQueuing.setText("Learn queuing")
          //TODO enter s into db
        }
        else {
          btnLearnQueuing.setText("Stop learn queuing")
        }
      }}
  }
}
