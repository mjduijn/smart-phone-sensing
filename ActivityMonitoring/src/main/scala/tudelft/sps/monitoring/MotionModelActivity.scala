package tudelft.sps.monitoring

import android.app.Activity
import android.graphics._
import android.os.{PersistableBundle, Bundle}
import android.preference.PreferenceManager
import android.util.Log
import android.view.{WindowManager, View}
import android.view.View.OnClickListener
import android.widget.ImageView.ScaleType
import android.widget.{ImageView, Button, TextView}
import com.androidplot.xy.{BoundaryMode, LineAndPointFormatter, XYPlot, SimpleXYSeries}
import tudelft.sps.monitoring.MotionModelActivity
import tudelft.sps.statistics.{Classifier, Knn}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import rx.lang.scala.{Subscriber, Observable}
import rx.lang.scala.schedulers.ExecutionContextScheduler
import tudelft.sps.observable.{ManagedSubscriptions, ObservableAccelerometer}
import tudelft.sps.statistics.SeqExtensions._
import tudelft.sps.observable._
import tudelft.sps.data._
import tudelft.sps.observable.ViewObservable._
import tudelft.sps.lib.db._

import scala.concurrent.ExecutionContext.Implicits._

class MotionModelActivity extends Activity
  with ObservableAccelerometer
  with ManagedSubscriptions
  with ObservableCompass
{

  val TAG = "MotionModelActivity"


  var plot:XYPlot = null
  var series:SimpleXYSeries = null

  val tMin = 20
  val tMax = 50

  import MotionState.MotionState
  object MotionState extends Enumeration{
    type MotionState = Value
    val Walking = Value("Walking")
    val Queueing = Value("Queueing")
  }

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



  val stdevMagnitude = accelerometer
    .map(_.magnitude)
    .slider(25)
    .map(SeqMath.stdev(_))


  val walkTable = "WalkingAcceleration"
  val queueTable = "QueueAcceleration"
  val dbCreateQuery = Seq(
      s"""
          |CREATE TABLE $walkTable(
          | stdev REAL,
          | alpha REAL
        |);
        """.stripMargin,
      s"""
          |CREATE TABLE $queueTable(
          | stdev REAL,
          | alpha REAL
          |);
        """.stripMargin
  )

  var database:ObservableDBHelper = null
  var classifier:Classifier[KnnData, MotionState] = null

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_motion_model)
    database = ObservableDBHelper(this, "MotionModel.db", 1, dbCreateQuery)
    plot = findViewById(R.id.plot).asInstanceOf[XYPlot]
    plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED)
    series = new SimpleXYSeries("AutoCorrelation")
    plot.addSeries(series, new LineAndPointFormatter())


    val mapping: CursorSelector => KnnData = c => KnnData(c.getDouble("stdev"), c.getDouble("alpha"))

    val data = database.getReadableDatabase(){ db =>
      val walks = db.mapped(mapping)(s"SELECT * FROM $walkTable", null)
        .map((_, MotionState.Walking))
      val queues = db.mapped(mapping)(s"SELECT * FROM $queueTable", null)
        .map((_, MotionState.Queueing))
      (KnnData.empty, MotionState.Queueing) :: (walks.merge(queues).toBlocking.toList)
    }
    classifier = Knn.traversableToKnn(data).toKnn(5, (a, b) => a.distance(b))
  }

  override def onResume(): Unit = {
    super.onResume()

    autoCorrelation
      .slider(20)
      .observeOn(ExecutionContextScheduler(global))
      .subscribeRunning { seq =>
        series.setModel(seq.map(_._2.asInstanceOf[java.lang.Double]).asJava, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY)
        plot.redraw()
      }


    val textStepInterval = findViewById(R.id.textStepInterval).asInstanceOf[TextView]


    val tau = ((30, 1d, 0d) +: autoCorrelation)
      .observeOn(ExecutionContextScheduler(global))
      .filter(_._2 > 0.7)
      .map(_._1)
      .slider(20)
      .map(x => SeqMath.alphaTrimmer(x.map(_.toDouble), 0.2))

    tau
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{x =>
        textStepInterval.setText("%.2f tau".format(x))
      }

    val magnitudes =
      accelerometer
      .observeOn(ExecutionContextScheduler(global))
      .map(_.magnitude)
      .slidingBuffer(20, 5)

    val textStdevAcc = findViewById(R.id.textStdevAcc).asInstanceOf[TextView]
    //    autoCorrelation
    //      .observeOn(UIThreadScheduler(this))
    //      .subscribeRunning{ x =>
    //        textStdevAcc.setText("%.3f".format(x._3))
    //      }
    magnitudes
      .map(SeqMath.stdev(_))
      .observeOn(UIThreadScheduler(this))
      .subscribe(x => textStdevAcc.setText("%.3f".format(x)))

    val textAvgAcc = findViewById(R.id.textAvgAcc).asInstanceOf[TextView]
    magnitudes
      .map(SeqMath.mean(_))
      .observeOn(UIThreadScheduler(this))
      .subscribe(x => textAvgAcc.setText("%.3f".format(x)))

    val textAlphaTrimmerAcc = findViewById(R.id.textAlphaTrimAcc).asInstanceOf[TextView]
    magnitudes
      .map(SeqMath.alphaTrimmer(_, 0.1))
      .observeOn(UIThreadScheduler(this))
      .subscribe(x => textAlphaTrimmerAcc.setText("%.3f".format(x)))

    val textSamplingRate = findViewById(R.id.textSamplingRate).asInstanceOf[TextView]
    accelerometer
      .observeOn(ExecutionContextScheduler(global))
      .map(_ => System.currentTimeMillis())
      .zipWithPrevious
      .map(t => (t._2 - t._1))
      .slidingBuffer(25, 25)
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ diff =>
        val hertz = 1000 / diff.mean
        textSamplingRate.setText("%.1fHz".format(hertz))
      }

    val motionState = magnitudes
    .map{values =>
      val stdev = values.stdev
      val alpha = SeqMath.alphaTrimmer(values, 0.1)
      classifier.classify(KnnData(stdev, alpha))
    }
    .slidingBuffer(3, 1)
    .map{ x =>
      if(x.count(_.equals(MotionState.Walking)) > 1) MotionState.Walking else MotionState.Queueing
    }

    val textState = findViewById(R.id.textState).asInstanceOf[TextView]
    motionState
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning(x => textState.setText(x.toString))


    /////Learned metrics part
    val btnStartStop = findViewById(R.id.btn_start_stop).asInstanceOf[Button]
    val btnWalking = findViewById(R.id.btn_walking).asInstanceOf[Button]
    val btnQueueing = findViewById(R.id.btn_queueing).asInstanceOf[Button]
    //Add onclick listeners
    val startStopObs= btnStartStop.onClick
    .toggle()
    .doOnEach(if(_){
      btnStartStop.setText("Start")
    } else {
      btnStartStop.setText("Stop")
    })


    def train(obs:Observable[Boolean], table:String): Unit = obs
      .observeOn(ExecutionContextScheduler(global))
      .doOnEach(if(_){database.getWritableDatabase().delete(table, null, null)})
      .combineLatestWith(magnitudes)((b, e) => (b, e))
      .filter(_._1)
      .map(_._2)
//      .slidingBuffer(50, 10)
      .subscribeRunning{ points =>
        val stdev = points.stdev
        val alpha = SeqMath.alphaTrimmer(points, 0.1)
        database.getWritableDatabase(){ db =>
          db.insertRow(table,
            "stdev" -> stdev,
            "alpha" -> alpha
          )
        }
      }

    val btnTrainWalking = findViewById(R.id.btn_train_walking).asInstanceOf[Button]
    train(btnTrainWalking.onClick.toggle(false, true).doOnEach(b => btnTrainWalking.setText(if(b) "Stop Training" else "Train Walking")), walkTable)
    val btnTrainQueueing = findViewById(R.id.btn_train_queueing).asInstanceOf[Button]
    train(btnTrainQueueing.onClick.toggle(false, true).doOnEach(b => btnTrainQueueing.setText(if(b) "Stop Training" else "Train Queueing")), queueTable)

    val walkingObs = Observable((aSubscriber: Subscriber[String]) => {
      btnWalking.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext("Walking")
      })
    })
    val queueingObs = Observable((aSubscriber: Subscriber[String]) => {
      btnQueueing.setOnClickListener(new OnClickListener {
        override def onClick(p1: View): Unit = if(!aSubscriber.isUnsubscribed) aSubscriber.onNext("Queueing")
      })
    })

    //TODO calculate times and stuff
    var testingTimer = System.currentTimeMillis()
    startStopObs
      .merge(walkingObs)
        .merge(queueingObs)
        .subscribe(state => {
    })

    val floormap = FloorMap(10000)



    val b  = Bitmap.createBitmap(720, 143, Bitmap.Config.ARGB_8888)
//    val b = Bitmap.createBitmap(b0, 0, 0, 143, 720, matrix, true)
    val canvas = new Canvas(b)
    val iv = this.findViewById(R.id.image_floor_plan).asInstanceOf[ImageView]

    iv.setImageBitmap(b)

    val matrix = new Matrix()
    iv.setScaleType(ScaleType.MATRIX)   //required
    matrix.postScale(1.75.toFloat, 4)
    matrix.postRotate(90, 720/2, 720/2)

//    matrix.postRotate(90, 720/2, 720/2)
//    matrix.postScale(4, 1)

    iv.setImageMatrix(matrix)

    val lines = floormap.walls
    val paint = new Paint()

//    var angle:Float = 0.0f


    case class MovementData(compass:Float, state:MotionState, tau:Double)


    val angleDiff = 25/180 * Math.PI
    val movementData = compass
      .combineLatest(motionState)
      .combineLatest(tau)
      .sample(200 millis)
      .map(x => MovementData(x._1._1, x._1._2, x._2))

    movementData
      .filter(_.state.equals(MotionState.Walking))
      .doOnEach{ data =>
        floormap.move(1, (data.compass + angleDiff).toFloat)
        canvas.drawColor(Color.WHITE)
        paint.setColor(Color.BLACK)
        for (i <- lines.indices) {
          canvas.drawLine(lines(i).x0 / 100, lines(i).y0 / 100, lines(i).x1 / 100, lines(i).y1 / 100, paint)
        }

        paint.setColor(Color.BLUE)
        for(p <- floormap.particles){
          canvas.drawPoint(p.x / 100, p.y / 100, paint)
        }
        paint.setColor(Color.RED)
        for(d <- floormap.deadZones){
          canvas.drawCircle(d._1 / 100, d._2 /100, 10, paint)
        }
        paint.setColor(Color.BLUE)
        for(p <- floormap.particles) {
          canvas.drawPoint(p.x / 100, p.y / 100, paint)
        }
      }
      .observeOn(UIThreadScheduler(this))
      .subscribeRunning{ data =>
        iv.invalidate()
      }

    val textCompass = findViewById(R.id.textCompass).asInstanceOf[TextView]
    compass.subscribeRunning{ x =>
      textCompass.setText("%.2f".format(x))
    }
  }
}