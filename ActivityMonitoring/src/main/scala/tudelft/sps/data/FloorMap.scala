package tudelft.sps.data

import org.json._
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.subjects.PublishSubject

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits._
import scalaj.http._
import java.io.{IOException, OutputStreamWriter, FileOutputStream, File}


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

  val TAG = "FloorMap"

  private var current = particles1
  private var old = particles2
  def particles = current

  var clusters = List[Cluster]()
  private val subj = PublishSubject[Int]
  val drawObs:Observable[Int] = subj.subscribeOn((ExecutionContextScheduler(global))).onBackpressureDrop

  private def swap() =  {
    current = if(current.equals(particles1)) particles2 else particles1
    old = if(old.equals(particles1)) particles2 else particles1
  }

  for(i <- 0 until particleCount){


    var randomParticle = Particle(
      x = random.nextInt(width + 1),
      y = random.nextInt(height + 1),
//      compassError = (random.nextDouble() - 0.5) * 0.25,
      compassError = random.nextGaussian() * 0.1,
      strideError = (random.nextDouble() - 0.5) * 0.5
    )

    while(deadZones.exists{case (x,y) => !walls.exists(wall => wall.doLinesIntersect(x, randomParticle.x, y, randomParticle.y))}){
      randomParticle = Particle(
        x = random.nextInt(width + 1),
        y = random.nextInt(height + 1),
//        compassError = (random.nextDouble() - 0.5) * 0.25,
        compassError = random.nextGaussian() * 0.1,
        strideError = (random.nextDouble() - 0.5) * 0.5
      )
    }
//    println("Generated particle")
    particles1(i) = randomParticle
    particles2(i) = randomParticle.copy()
  }

  /**
   *
   * @param distance length of stride in mm
   */
  def move(distance: Int, angle: Double) = {
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

      val stride = distance + distance * current(i).strideError
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
    sendParticles()
  }

  def sendParticles():Unit = {
    println("Sending request!")
    val url = "http://mc.besuikerd.com:8000/"

    val sb = new StringBuilder()
    current.foreach(p => sb.append(p.x + " " + p.y + "\n")) //TODO do properly

    Observable[Unit](sub => {
      try{
        val response = Http(url).timeout(500, 2000).postData(sb.toString()).header("content-type", "text/html").asString
        val jArray = new JSONArray(response.body)
        val lb = ListBuffer[Cluster]()
        for (i <- 0 until jArray.length()) {
          val obj = jArray.getJSONObject(i)
          val cluster = Cluster(obj.getInt("x"), obj.getInt("y"), obj.getInt("covarX"), obj.getInt("covarY"), obj.getDouble("weight"))
          lb.append(cluster)
        }
        clusters = lb.sortBy(x => x.weight).toList
      } catch {
        case e: IOException =>
        case e: JSONException =>
      }
      sub.onNext(())
      }).subscribeOn(ExecutionContextScheduler(global))
//      .just(Http(url).postData(sb.toString()).header("content-type", "text/html").asString)
//      .doOnEach(response => {
//        try {
//          val jArray = new JSONArray(response.body)
//          val lb = ListBuffer[Cluster]()
//          for (i <- 0 until jArray.length()) {
//            val obj = jArray.getJSONObject(i)
//            val cluster = Cluster(obj.getInt("x"), obj.getInt("y"), obj.getInt("covarX"), obj.getInt("covarY"), obj.getDouble("weight"))
//            lb.append(cluster)
//          }
//          clusters = lb.sortBy(x => x.weight).toList
//        } catch {
//          case e => Log.d(TAG, "Exception while parsing json \n" + e)(
//        }
//      })
      .foreach(_ => subj.onNext(1))


//    val response = Http(url).postData(sb.toString()).header("content-type", "text/html").asString
//    try {
//
//      val jArray = new JSONArray(response.body)
//      for (i <- 0 until jArray.length()) {
//        val obj = jArray.getJSONObject(i)
//        val cluster = Cluster(obj.getInt("x"), obj.getInt("y"), obj.getInt("covarX"), obj.getInt("covarY"), obj.getDouble("weight"))
//      }
//    } catch {
//      case e => Log.d(TAG, "Exception while parsing json \n" + e)
//    }
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