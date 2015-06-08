package tudelft.sps.data

object MotionState extends Enumeration{
  type MotionState = Value
  val Walking = Value("Walking")
  val Queueing = Value("Queueing")
}