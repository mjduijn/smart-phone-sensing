package tudelft.sps.lib.widget

import android.content.Context
import android.graphics._
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.ExecutionContextScheduler
import tudelft.sps.data.{Cluster, FloorMap}
import tudelft.sps.lib.util.FunctionRunnable

import scala.concurrent.ExecutionContext

class FloorMapView(ctx:Context, attrs:AttributeSet, defStyleAttr:Int) extends ImageView(ctx, attrs, defStyleAttr) {

  def this(context:Context, attrs:AttributeSet) = this(context, attrs, 0)
  def this(context:Context) = this(context, null)

  var floorMap:FloorMap = null

  private val paint = new Paint()
  private val bitmap = Bitmap.createBitmap(143, 720, Bitmap.Config.ARGB_8888)
  private val canvas = new Canvas(bitmap)

  setImageBitmap(bitmap)

  if(isInEditMode){
    floorMap = FloorMap.apply(1000, .1d)
    floorMap.clusters = List(Cluster(FloorMap.standardWidth / 2, FloorMap.standardHeight / 2, 1000000, 10000000, 1))
    redraw()
  }

  def redraw(): Unit ={
    val lines = floorMap.walls
    canvas.drawColor(Color.WHITE)
    paint.setColor(Color.BLACK)
    for (i <- lines.indices) {
      canvas.drawLine(lines(i).x0 / 100, lines(i).y0 / 100, lines(i).x1 / 100, lines(i).y1 / 100, paint)
    }

    paint.setColor(Color.BLUE)
    for(p <- floorMap.particles){
      canvas.drawPoint(p.x / 100, p.y / 100, paint)
    }
    paint.setColor(Color.BLUE)
    for(p <- floorMap.particles) {
      canvas.drawPoint(p.x / 100, p.y / 100, paint)
    }

    //Draw clusters
    for(i <- floorMap.clusters.indices) {
      if(i == 0) {
        paint.setColor(Color.RED)
      }
      else {
        paint.setColor(Color.GRAY)
      }
      val cluster = floorMap.clusters(i)
      val left = cluster.x - cluster.covarX / 2000
      val right = cluster.x + cluster.covarX / 2000
      val top = cluster.y - cluster.covarY / 2000
      val bottom = cluster.y + cluster.covarY / 2000

      paint.setAlpha((cluster.weight * 255).toInt)
      canvas.drawOval(left / 100, top / 100, right / 100, bottom / 100, paint)
    }
    paint.setAlpha(255)
    postInvalidate()
  }
}