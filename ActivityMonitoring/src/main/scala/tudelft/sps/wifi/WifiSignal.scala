package tudelft.sps.wifi

case class WifiSignal(
  mac:String,
  ssid:String,
  frequency:Int,
  rssi:Int
)