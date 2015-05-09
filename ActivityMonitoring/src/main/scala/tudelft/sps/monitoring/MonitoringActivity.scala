package tudelft.sps.monitoring

import android.app.Activity
import android.content.ContentValues
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.app.Fragment
import android.support.v4.app.ActionBarDrawerToggle
import android.support.v4.view.GravityCompat
import android.util.Log
import android.view.{Menu, ViewGroup, LayoutInflater, View}
import android.view.View.OnClickListener
import android.widget._
import com.androidplot.xy.{LineAndPointFormatter, SimpleXYSeries, XYSeries, XYPlot}
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.{Subscriber, Observer, Observable}
import tudelft.sps.data.Acceleration
import tudelft.sps.observable._
import scala.collection.JavaConverters._
import rx.lang.scala.Observable
import tudelft.sps.wifi.{WifiSignal, ObservableWifiManager}
import scala.concurrent.duration._
import tudelft.sps.statistics.{Classifier, Knn}
import tudelft.sps.lib.db._
import scala.concurrent.ExecutionContext.Implicits._
import android.support.v4.widget.DrawerLayout

class MonitoringActivity extends Activity
  with ObservableAccelerometer
  with ObservableWifiManager
  with ManagedSubscriptions
{
  private val TAG = getClass.getSimpleName()

  private var mDrawerLayout: DrawerLayout = null
  private val tabNames = List("Testing", "Evaluation")
  private var mDrawerList: ListView = null
  private var mDrawerToggle: ActionBarDrawerToggle = null
  private var mTitle: CharSequence = ""


  var classifier: Classifier[Acceleration, Int] = null
  var dbHelper:ObservableDBHelper = null
  val walkTable = "WalkingAcceleration"
  val queueTable = "QueueAcceleration"

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
//    setContentView(R.layout.activity_hello)
    setContentView(R.layout.activity_main)

    mDrawerLayout = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
    mDrawerList = findViewById(R.id.left_drawer).asInstanceOf[ListView]
    mDrawerList.setAdapter(new ArrayAdapter[String](this, R.layout.drawer_list_item, tabNames.toArray))
    mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) = {
        selectItem(position)
      }
    })

    mDrawerToggle = new ActionBarDrawerToggle(
        this,                  /* host Activity */
        mDrawerLayout,         /* DrawerLayout object */
        R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
        R.string.drawer_open,  /* "open drawer" description for accessibility */
        R.string.drawer_close  /* "close drawer" description for accessibility */
    ) {
      override def onDrawerClosed(view: View) = {
        getActionBar().setTitle(mTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }
      override def onDrawerOpened(drawerView: View) = {
        getActionBar().setTitle(mTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }
    }
    mDrawerLayout.setDrawerListener(mDrawerToggle);

    if (savedInstanceState == null) {
      selectItem(0);
    }

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

  override def onCreateOptionsMenu(menu: Menu) = {
    val inflater = getMenuInflater()
    inflater.inflate(R.menu.main, menu)
    super.onCreateOptionsMenu(menu)
  }

//  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
//    val drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList)
//    menu.findItem(R.id.action_websearch).setVisible(!drawerOpen)
//    super.onPrepareOptionsMenu(menu)
//  }

  /**
   * When using the ActionBarDrawerToggle, you must call it during
   * onPostCreate() and onConfigurationChanged()...
   */
  override def onPostCreate(savedInstanceState: Bundle) = {
    super.onPostCreate(savedInstanceState)
    mDrawerToggle.syncState()
  }
  override def onConfigurationChanged(newConfig: Configuration) = {
    super.onConfigurationChanged(newConfig);
    // Pass any configuration change to the drawer toggls
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  def selectItem(position: Int): Unit = {
    // update the main content by replacing fragments
    val fragment: Fragment = new TrainFragment();
//    val args = new Bundle();
//
//    args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//    fragment.setArguments(args);

    val fragmentManager = getFragmentManager();
    fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

    // update selected item and title, then close the drawer
    mDrawerList.setItemChecked(position, true);
    setTitle(tabNames(position));
    mDrawerLayout.closeDrawer(mDrawerList);
  }

  override def setTitle(title: CharSequence): Unit = {
    mTitle = title
    getActionBar().setTitle(mTitle)
  }

  override def onResume(): Unit = {
    super.onResume()

    val btnLearnWalking = findViewById(R.id.btn_learn_walking).asInstanceOf[Button]

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
      .foreach{ acc =>
        dbHelper.getWritableDatabase(){ db =>
            db.insertRow(walkTable,
              "x" -> acc.x,
              "y" -> acc.y,
              "z" -> acc.z
            )
          }
        }




    val btnLearnQueuing = findViewById(R.id.btn_learn_queuing).asInstanceOf[Button]
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

  class TrainFragment extends Fragment {
    def TrainFragment() {
      // Empty constructor required for fragment subclasses
    }

    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
      val rootView: View = inflater.inflate(R.layout.fragment_planet, container, false)
      rootView
    }
  }
}
