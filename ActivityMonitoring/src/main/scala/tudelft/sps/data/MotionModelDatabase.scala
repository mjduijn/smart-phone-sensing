package tudelft.sps.data

import android.app.Activity
import android.os.Bundle
import tudelft.sps.lib.db.ObservableDBHelper

trait MotionModelDatabase extends Activity{
  val walkTable = "WalkingAcceleration"
  val queueTable = "QueueAcceleration"


  private var _motionModelDatabase:ObservableDBHelper = null
  def motionModelDatabase = _motionModelDatabase


  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    _motionModelDatabase = ObservableDBHelper(this, "MotionModel.db", 1, motionModelDbCreateQuery)
  }

  val motionModelDbCreateQuery = Seq(
    s"""
          |CREATE TABLE $walkTable(
          | stdev REAL,
          | alpha REAL
        |);
        """.stripMargin,
    s"""
          |CREATE TABLE $queueTable(
          | stdev REAL,
          | alpha REAL
          |);
        """.stripMargin
  )
}