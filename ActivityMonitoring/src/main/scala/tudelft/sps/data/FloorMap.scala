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
  private val deadParticles = new Array[Particle](particleCount)
  private val aliveParticles = new Array[Particle](particleCount)

  private val random = new Random()

  private var current = particles1
  private var old = particles2
  def particles = current

  private def swap() =  {
    current = if(current.equals(particles1)) particles2 else particles1
    old = if(old.equals(particles1)) particles2 else particles1
  }

  for(i <- 0 until particleCount){


    var randomParticle = Particle(
      x = random.nextInt(width + 1),
      y = random.nextInt(height + 1),
      compassError = (random.nextDouble() - 0.5) * 0.25,
      strideError = (random.nextDouble() - 0.5) * 0.5
    )

    while(deadZones.exists{case (x,y) => !walls.exists(wall => wall.doLinesIntersect(x, randomParticle.x, y, randomParticle.y))}){
      randomParticle = Particle(
        x = random.nextInt(width + 1),
        y = random.nextInt(height + 1),
//        compassError = random.nextGaussian(),
        compassError = (random.nextDouble() - 0.5) * 0.25,
        strideError = (random.nextDouble() - 0.5) * 0.5
      )
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
    var deadCount = 0
    var aliveCount = 0
    for (i <- current.indices) {
      //TODO paper says compass error should be Gaussian

      var compassAngle = angle + Math.PI * current(i).compassError
      while (compassAngle < -Math.PI) {
        compassAngle += 2 * Math.PI
      }
      while (compassAngle > Math.PI) {
        compassAngle -= 2 * Math.PI
      }

      val stride = strideLength + strideLength * current(i).strideError
      old(i).x = (current(i).x + stride * Math.cos(compassAngle)).toInt
      old(i).y = (current(i).y + stride * Math.sin(compassAngle)).toInt

      var dead = false
//      for(wall <- walls) {
//        if(wall.doLinesIntersect(old(i).x, current(i).x, old(i).y, current(i).y)) {
//          dead = true
//        }
//      }
      if (walls.exists(wall => wall.doLinesIntersect(old(i).x, current(i).x, old(i).y, current(i).y))) {
        dead = true
      }

      if(deadZones.exists{case (x,y) => !walls.exists(wall => wall.doLinesIntersect(x, old(i).x, y, old(i).y))}){
        dead = true
      }

      //      if (walls.exists(wall => wall.doLinesIntersect(old(i).x, current(i).x, old(i).y, current(i).y))) {
      if(dead) {
        deadParticles(deadCount) = old(i)
        deadCount += 1
      } else {
        aliveParticles(aliveCount) = current(i)
        aliveCount += 1
      }
    }
    if(aliveCount > 0){
      for(i <- 0 until deadCount){
        val randomPoint = random.nextInt(aliveCount)
        deadParticles(i).x = aliveParticles(randomPoint).x
        deadParticles(i).y = aliveParticles(randomPoint).y
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