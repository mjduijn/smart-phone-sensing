package tudelft.sps.wifi

import android.app.Activity
import android.content.{IntentFilter, Intent, Context, BroadcastReceiver}
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.JavaConversions

trait ObservableWifiManager extends Activity{
  private val TAG = "ObservableWifiManager"

  private val wifiScansSubject = BehaviorSubject[Seq[WifiSignal]]()
  val wifiScans:Observable[Seq[WifiSignal]] = wifiScansSubject

  private var wifiManager:WifiManager = null

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    wifiManager = getSystemService(Context.WIFI_SERVICE).asInstanceOf[WifiManager]
  }

  override def onResume(): Unit = {
    super.onResume()
    registerReceiver(WifiBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
  }

  override def onPause(): Unit = {
    super.onPause()
    unregisterReceiver(WifiBroadcastReceiver)
  }

  object WifiBroadcastReceiver extends BroadcastReceiver{
    override def onReceive(ctx: Context, intent: Intent): Unit = {
      Log.d(TAG, "Wifi Scan completed")
      val results = JavaConversions.asScalaBuffer(wifiManager.getScanResults())
        .map(result => WifiSignal(result.BSSID, result.SSID, result.frequency, result.level))
      wifiScansSubject.onNext(results)
    }
  }

  def startWifiscan(): Unit ={
    Log.d(TAG, "Starting Wifi scan..")
    wifiManager.startScan()
  }
}