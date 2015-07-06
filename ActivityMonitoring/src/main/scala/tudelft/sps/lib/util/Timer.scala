package tudelft.sps.lib.util

object Timer {
  def timed(f: => Unit):Long = {
    val time = System.currentTimeMillis();
    f
    System.currentTimeMillis() - time
  }
}
