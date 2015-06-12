package tudelft.sps.lib.util

case class FunctionRunnable(f: () => Unit) extends Runnable{
  override def run(){f()}
}