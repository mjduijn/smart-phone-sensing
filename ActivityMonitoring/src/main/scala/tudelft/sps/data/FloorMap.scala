package tudelft.sps.data

import scala.util.Random

class FloorMap(
  val particleCount:Int,
  val width: Int,
  val height: Int,
  val walls:Array[Line],
  val deadZones:Array[(Int, Int)]
){
  private val particles1 = new Array[Particle](particleCount)
  private val particles2 = new Array[Particle](particleCount)
  private val random = new Random()

  private var current = particles1
  private var old = particles2
  def particles = current

  private def swap() =  {
    current = if(current.equals(particles1)) particles2 else particles1
    old = if(old.equals(particles1)) particles2 else particles1
  }

  for(i <- 0 until particleCount){
    var randomParticle = Particle(random.nextInt(width + 1), random.nextInt(height + 1), 0, 0)

    while(deadZones.exists{case (x,y) => !walls.exists(wall => wall.doLinesIntersect(x, randomParticle.x, y, randomParticle.y))}){
      randomParticle = Particle(random.nextInt(width + 1), random.nextInt(height + 1), 0, 0)
    }
//    println("Generated particle")
    particles1(i) = randomParticle
    particles2(i) = randomParticle.copy()
  }

  /**
   *
   * @param strideLength length of stride in mm
   */
  def move(strideLength: Int, angle: Float) = {
    val compassError = 0.0 // beta_i TODO should be a gaussian error
    val placementOffset = 0.0 //alpha_i since phone is kept straight ahead, should always be 0
//    val angleRad = ((angle * Math.PI) / 180)

    println(angle)
    for(i <- current.indices) {
      //TODO paper says compass error should be Gaussian
      val compassAngle = angle + (random.nextFloat() - 0.5) * angle
      val strideLengthError = ((random.nextInt(strideLength * 2) - strideLength) * 0.1).toInt // delta_i up to 10% of stride length
      val stride = strideLength + strideLengthError
      old(i).x = (current(i).x + stride * Math.cos(compassAngle)).toInt
      old(i).y = (current(i).y + stride * Math.sin(compassAngle)).toInt

      for{
        wall <- walls if wall.doLinesIntersect(old(i).x, current(i).x, old(i).y, current(i).y)
      }{
        val randomPoint = random.nextInt(particleCount)
        old(i).x = current(randomPoint).x
        old(i).y = current(randomPoint).y
      }
    }
    swap()
  }
}

object FloorMap{
  val nrOfWalls = 20
  val scaling = 1000

  val standardWidth = 72000
  val standardHeight = 14300

  def apply(particleCount:Int):FloorMap = new FloorMap(particleCount, standardWidth, standardHeight, create9th(), deadZones)

  def toCo(c: Double):Int = (c * scaling).toInt

  def mkline(x0: Double, x1: Double, y0: Double, y1: Double):Line =
    Line(toCo(x0), toCo(x1), toCo(y0), toCo(y1))

  def create9th(): Array[Line] = {
    val result = scala.collection.mutable.MutableList[Line]()

    //Room 1
    result += mkline(0.0, 0, 0, 6.1)
    result += mkline(0.0, 8, 0, 0)
    result += mkline(8.0, 8, 0, 6.1)

    //Room 2
    result += mkline(12.0, 12, 0, 6.1)
    result += mkline(12.0, 16, 0, 0)
    result += mkline(16.0, 16.0, 0, 6.1)

    //Room 3
    result += mkline(16.0, 20, 0, 0)
    result += mkline(20.0, 20, 0, 6.1)

    //Room 4
    //    result += mkline(12.0, 12, 11.3, 14.3)
    result += mkline(12.0, 12, 8.2, 14.3)
    result += mkline(12.0, 16, 14.3, 14.3)
    //    result += mkline(16.0, 16, 11.3, 14.3)
    result += mkline(16.0, 16, 8.2, 14.3)

    //Room 5
    result += mkline(56.0, 56.0, 8.2, 14.3)
    result += mkline(56.0, 60.0, 14.3, 14.3)
    result += mkline(60.0, 60.0, 8.2, 14.3)

    //Room 6
    result += mkline(60.0, 64.0, 14.3, 14.3)
    result += mkline(64.0, 64.0, 8.2, 14.3)

    //Central hallway
    result += mkline(8.0, 12.0, 6.1, 6.1) //Upper
    result += mkline(20.0, 72.0, 6.1, 6.1)

    result += mkline(0, 0, 6.1, 8.1) //Vertical
    result += mkline(72, 72, 6.1, 8.1)

    result += mkline(0.0, 12.0, 8.2, 8.2) //Lower
    result += mkline(16.0, 56.0, 8.2, 8.2)
    result += mkline(64.0, 72.0, 8.2, 8.2)

    //TODO elevator??

    return result.toArray
  }

  val deadZones = Array[(Int, Int)](
    (10000, 2000),
    (70000, 3000),
    (6000, 10000),
    (25000, 10000),
    (70000, 10000)
  )
}