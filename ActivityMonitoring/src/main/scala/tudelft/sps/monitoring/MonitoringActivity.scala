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
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.{Subscriber, Observer, Observable}
import tudelft.sps.data.Acceleration
import tudelft.sps.observable._
import scala.collection.JavaConverters._
import android.widget.{ListView, TextView}
import rx.lang.scala.Observable
import tudelft.sps.wifi.{WifiSignal, ObservableWifiManager}
import scala.concurrent.duration._
import tudelft.sps.statistics.{SeqExtensions, Classifier, Knn}
import tudelft.sps.lib.db._
import scala.concurrent.ExecutionContext.Implicits._
import tudelft.sps.observable._

class MonitoringActivity extends Activity
  with ObservableAccelerometer
  with ObservableWifiManager
  with ManagedSubscriptions
{
  private val TAG = getClass.getSimpleName()


  var classifier: Classifier[Acceleration, Int] = null
  var dbHelper:ObservableDBHelper = null
  val walkTable = "WalkingAcceleration"
  val queueTable = "QueueAcceleration"

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_hello)

    dbHelper = ObservableDBHelper.apply(this, "monitoring.db", 1,
      Seq(
        s"""
          |CREATE TABLE $walkTable(
          | x REAL,
          | y REAL,
          | z REAL
        |);
        """.stripMargin,
        s"""
          |CREATE TABLE $queueTable(
          | x REAL,
          | y REAL,
          | z REAL
          |);
        """.stripMargin
      )
    )

    val mapping : CursorSelector => Acceleration = selector => Acceleration(selector.getFloat("x"), selector.getFloat("y"), selector.getFloat("z"))
    var data:List[(Acceleration, Int)] = null
    dbHelper.getReadableDatabase(){ db =>
      val walks = db.mapped(mapping)(s"SELECT * FROM $walkTable", null)
        .map((_, 1))
      val queues = db.mapped(mapping)(s"SELECT * FROM $queueTable", null)
        .map((_, 0))
      data = (Acceleration(0,0, 0), 0) :: walks.merge(queues).toBlocking.toList
    }
    classifier = Knn.traversableToKnn(data).toKnn(5, (a, b) => a.distance(b))
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
      .subscribeRunning{x =>
        findViewById(R.id.accelerometer_value).asInstanceOf[TextView].setText("Accelerometer: %.2f".format(x))
      }

//    accelerometerSum
//      .map(sum => if (sum > 3) "Walking" else "Queueing")
//      .observeOn(UIThreadScheduler(this))
//      .subscribeManaged{guess =>
//        findViewById(R.id.activity_guess).asInstanceOf[TextView].setText(guess)
//      }


    import SeqExtensions._
    accelerometer
      .map(Acceleration.apply)
      .slidingBuffer(20, 1)
//      .flatMap{sample => if(sample.isEmpty) Observable.empty else Observable.just(sample.sortBy(x => x.magnitude).apply(sample.size / 2))} //get median
      .flatMap{sample => if(sample.isEmpty) Observable.empty else Observable.just(Math.sqrt(sample.map(_.magnitude).variance))} //get stdev
      //.map(a => classifier.classify(a))
      //.doOnEach(println(_))
//      .slidingBuffer(6, 1)
//      .map(list => if(list.count(_.equals(1)) >= 2) 1 else 0)
      .flatMap(v => if (v < 1) Observable.just("Queueing") else if(v > 2) Observable.just("Walking") else Observable.empty)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{guess =>
        findViewById(R.id.activity_guess).asInstanceOf[TextView].setText(guess)
      }

    //TODO ask if discrete time is correct
    val tMin = 0
    val tMax = 10
    val updateInterval = 10
    val autoCorrelation:Observable[Double] = accelerometer
      .observeOn(ExecutionContextScheduler(global))
      .map(Acceleration.apply)
      .slidingBuffer(tMax * 2, updateInterval)
      .sample(25 millis)
      .map{ sample =>
        val time = System.currentTimeMillis()
        def psi(m:Int) = {
          val magnitude = sample.map(_.magnitude)
          for (t_i <- tMin to (tMax - 1)) yield {
            val mean0 = magnitude.drop(m).take(t_i).mean
            val mean1 = magnitude.drop(m + t_i).take(t_i).mean
            val seq = for (k <- 0 to t_i - 1) yield
              (magnitude(m + k) - mean0) * (magnitude(m + k + t_i) - mean1)
            seq.sum / (t_i * magnitude.drop(m).take(t_i).variance * magnitude.drop(m + t_i).take(t_i).variance)
          }
        }
//        Log.d(TAG, "[%dms]autoCorrelation result: %.2f".format(System.currentTimeMillis() - time, psi(0).max))
        psi(0).max
      }

    val plot = findViewById(R.id.mySimpleXYPlot).asInstanceOf[XYPlot]
    val series1 = new SimpleXYSeries("Series 1")
    plot.addSeries(series1, new LineAndPointFormatter())

    var graphObservable = accelerometer
      //.observeOn(ExecutionContextScheduler(global))
      .map(Acceleration.apply(_))
      .slidingBuffer(20, 1)
      .map(_.map(_.magnitude).variance)

    graphObservable = autoCorrelation

    graphObservable
      .slider(20)
      //.observeOn(UIThreadScheduler(this))
      .subscribeRunning { b =>
        series1.setModel(b.map(_.asInstanceOf[java.lang.Double]).asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      }

    val adapter = ObservingListAdapter[WifiSignal, (TextView, TextView)](getApplicationContext, R.layout.signal_item){
      //for efficiency: findViewById is very expensive if it has to be done for each element, so these references are cached with this method
      view => (view.findViewById(R.id.mac).asInstanceOf[TextView], view.findViewById(R.id.rssi).asInstanceOf[TextView])
    }{ (holder, element) => //updates the view for the current element, using the viewholder
      holder._1.setText(element.mac)
      holder._2.setText(element.rssi.toString)
    }
    wifiScans
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning(adapter.onNext)

    findViewById(R.id.signals).asInstanceOf[ListView].setAdapter(adapter)

    Observable.just(-1) //to prevent initial delay
      .merge(Observable.interval(1 second))
      .subscribeRunning(_ => startWifiscan())

    val btnLearnWalking = findViewById(R.id.btn_learn_walking_nope).asInstanceOf[Button]

    //Add learn walking onclick listener
    Observable((aSubscriber: Subscriber[Int]) => {
      btnLearnWalking.setOnClickListener(new OnClickListener {
          override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext(1)
        //TODO deal with unsubscribe
      })
    }).scan(false)((x, i) => !x)
      .observeOn(UIThreadScheduler(this))
      .doOnEach { b =>
        if (b) {
          btnLearnWalking.setText("Learn walking")
        } else {
          btnLearnWalking.setText("Stop learn walking")
        }
      }
      .observeOn(ExecutionContextScheduler(global))
      .doOnEach(if(_){dbHelper.getWritableDatabase().delete(walkTable, null, null)})
      .combineLatestWith(accelerometer)((b, e) => (b, e))
      .filter(_._1)
      .map{case (b, e) => Acceleration(e.values(0), e.values(1), e.values(2))}
      .slidingBuffer(500 millis, 500 millis)
      .flatMap{sample => if(sample.isEmpty) Observable.empty else Observable.just(sample.sortBy(x => x.magnitude).apply(sample.size / 2))} //get median
      //.flatMap{sample => if(sample.isEmpty) Observable.empty else Observable.just(stdev(sample))} //get stdev
      .foreach{ acc =>
        dbHelper.getWritableDatabase(){ db =>
            db.insertRow(walkTable,
              "x" -> acc.x,
              "y" -> acc.y,
              "z" -> acc.z
            )
          }
        }




    val btnLearnQueuing = findViewById(R.id.btn_learn_queuing_nope).asInstanceOf[Button]
    //Add learn queuing onclick listener
    Observable((aSubscriber: Subscriber[Int]) => {
      btnLearnQueuing.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext(1)
      })
    })
      .scan(false)((x, i) => !x)
      .observeOn(UIThreadScheduler(this))
      .doOnEach{ b =>
        if(b) {
          btnLearnQueuing.setText("Learn queuing")
        } else {
          btnLearnQueuing.setText("Stop learn queuing")
        }
      }
      .observeOn(ExecutionContextScheduler(global))
      .doOnEach(if(_){dbHelper.getWritableDatabase().delete(queueTable, null, null)})
      .combineLatestWith(accelerometer)((b, e) => (b, e))
      .filter(_._1)
      .map{case (b, e) => Acceleration(e.values(0), e.values(1), e.values(2))}
      .slidingBuffer(500 millis, 500 millis)
      .flatMap{sample => if(sample.isEmpty) Observable.empty else Observable.just(sample.sortBy(x => x.magnitude).apply(sample.size / 2))} //get median
//      .flatMap{sample => if(sample.isEmpty) Observable.empty else Observable.just(stdev(sample))} //get stdev
      .foreach{ acc =>
      dbHelper.getWritableDatabase(){ db =>
          db.insertRow(queueTable,
            "x" -> acc.x,
            "y" -> acc.y,
            "z" -> acc.z
          )
        }
      }
  }
}
