package tudelft.sps.data

import android.util.Log
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
      x = random.nextInt(height + 1),
      y = random.nextInt(width + 1),
//      compassError = (random.nextDouble() - 0.5) * 0.25,
      compassError = random.nextGaussian() * 0.1,
      strideError = (random.nextDouble() - 0.5) * 0.5
    )

    while(deadZones.exists{case (x,y) => !walls.exists(wall => wall.doLinesIntersect(x, randomParticle.x, y, randomParticle.y))}){
      randomParticle = Particle(
        x = random.nextInt(height + 1),
        y = random.nextInt(width + 1),
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
        aliveParticles(aliveCount) = old(i)
        aliveCount += 1
      }
    }
//    if over 2/3 of particles died, sample "conservatively", choose a locations among all particles
    Log.d(TAG, "" + (deadCount.toDouble / particleCount))
    if(deadCount.toDouble / particleCount > 0.75) {
//      Log.d(TAG, "More than 90% dead particles")
      for(i <- 0 until deadCount){
        val randomPoint = random.nextInt(particleCount)
        deadParticles(i).x = current(randomPoint).x
        deadParticles(i).y = current(randomPoint).y
      }
    }else
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
        clusters = lb.sortBy(-_.weight).toList
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

    //Room 1 Dijkstrazaal
    result += mkline(8.2, 14.3, 0, 0)
    result += mkline(14.3, 14.3, 0, 8)
    result += mkline(8.2, 14.3, 8, 8)

    //Room 2 Chill room
    result += mkline(8.2, 14.3, 12, 12)
    result += mkline(14.3, 14.3, 12, 16)
    result += mkline(8.2, 14.3, 16, 16)

    //Room 3
    result += mkline(14.3, 14.3, 16, 20)
    result += mkline(8.2, 14.3, 20, 20)

    //Room 4 Coffee!
    result += mkline(3, 6.1, 14, 14)
    result += mkline(3, 3, 12, 14)
    result += mkline(0, 3, 12, 12)
    result += mkline(0, 0, 12, 16)
    result += mkline(0, 6.1, 16, 16)

    //Room 5 + 6
    result += mkline(0, 6.1, 56, 56)
    result += mkline(0, 0, 56, 64)
    result += mkline(0, 6.1, 60, 60)
    result += mkline(0, 6.1, 64, 64)

    //Central hallway
    result += mkline(8.2, 8.2, 8, 12) //Upper
    result += mkline(8.2, 8.2, 20, 72)

    result += mkline(6.1, 8.2, 0, 0) //Vertical
    result += mkline(6.1, 8.2, 72, 72)

    result += mkline(6.1, 6.1, 0, 14) //Lower
    result += mkline(6.1, 6.1, 16, 56)
    result += mkline(6.1, 6.1, 64, 72)

    return result.toArray
  }

  val deadZones = Array[(Int, Int)](
    (-1000, -1000), //Top left (2)
    (4000, -1000),
    (-1000, 30000), //middle left
    (-1000, 75000), //bottom left

    (15000, 10000), //top right
    (15000, 75000) //bottom right
  )
}