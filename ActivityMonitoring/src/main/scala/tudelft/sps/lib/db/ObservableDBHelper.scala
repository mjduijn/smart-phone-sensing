package tudelft.sps.lib

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import rx.lang.scala.Observable

/**
 * following is an example of how the database works:
 *
 *
    ObservableDBHelper(this, dbName, dbVersion, createQuery).getWritableDatabase(){ db =>
          db.transaction{
            db.rawQuery(s"DELETE FROM $vectorTable", null)
            for{
              x <- 1 to 10
              y <- 1 to 10
              z <- 1 to 10
            } db.insertRow(vectorTable,
              "x" -> x,
              "y" -> y,
              "z" -> z
            )
          }

          val values = db.mapped{ c =>
            (c.getInt("x"), c.getInt("y"), c.getInt("z"))
          }(s"SELECT * FROM $vectorTable", null)

          values.foreach(println)
        }

 */
package object db{
  abstract class Put[A](val putF: ContentValues => Unit)

  case class PutInt(key:String, value:Int) extends Put[Int](_.put(key, value.asInstanceOf[java.lang.Integer]))
  case class PutDouble(key:String, value:Double) extends Put[Double](_.put(key, value.asInstanceOf[java.lang.Double]))
  case class PutLong(key:String, value:Long) extends Put[Long](_.put(key, value.asInstanceOf[java.lang.Long]))
  case class PutFloat(key:String, value:Float) extends Put[Float](_.put(key, value.asInstanceOf[java.lang.Float]))
  case class PutBlob(key:String, value:Array[Byte]) extends Put[Array[Byte]](_.put(key, value))

  class CursorSelector(cursor:Cursor){
    def getBlob(s:String) = cursor.getBlob(cursor.getColumnIndex(s))
    def getFloat(s:String) = cursor.getFloat(cursor.getColumnIndex(s))
    def getDouble(s:String) = cursor.getDouble(cursor.getColumnIndex(s))
    def getString(s:String) = cursor.getString(cursor.getColumnIndex(s))
    def getInt(s:String) = cursor.getInt(cursor.getColumnIndex(s))
    def getLong(s:String) = cursor.getLong(cursor.getColumnIndex(s))
  }

  implicit def implicitPut(key:String) = new {
    def -> (i:Int): Put[Int] = PutInt(key, i)
    def -> (d:Double): Put[Double] = PutDouble(key, d)
    def -> (l:Long): Put[Long] = PutLong(key, l)
    def -> (f:Float): Put[Float] = PutFloat(key, f)
    def -> (blob:Array[Byte]): Put[Array[Byte]] = PutBlob(key, blob)
  }

  implicit class SQLiteDatabaseExtensions(val db:SQLiteDatabase) extends AnyVal {

    def apply(action: SQLiteDatabase => Unit): Unit ={
      try{
        action(db)
      } finally{
        db.close()
      }
    }

    def insertRow(table:String, values:Put[_]*){
      val c = new ContentValues()
      for(value <- values){
        value.putF(c)
      }
      db.insert(table, null, c)
    }

    def transaction(f: => Unit): Unit ={
      db.beginTransaction()
      try{
        f
        db.setTransactionSuccessful()
      } finally{
        db.endTransaction()
      }
    }

    def mapped[A](mapping: CursorSelector => A)(sql:String, selectionArgs:Array[String]):Observable[A] = {
      Observable[A]{ sub =>
        val cursor = db.rawQuery(sql, selectionArgs)
        val selector = new CursorSelector(cursor)
        while(cursor.moveToNext()){
          sub.onNext(mapping(selector))
        }
      }
    }
  }



  trait ObservableDBHelper extends SQLiteOpenHelper{
    val dbCreateQuery:String
    val ctx:Context

    abstract override def onCreate(db: SQLiteDatabase): Unit = {
      db.execSQL(dbCreateQuery)
    }

    abstract override def onUpgrade(db: SQLiteDatabase, p2: Int, p3: Int): Unit = {
    }
  }

  object ObservableDBHelper {
    private class ConcreteSQLiteOpenHelper(ctx:Context, dbName:String, dbVersion:Int) extends SQLiteOpenHelper(ctx, dbName, null, dbVersion){
      override def onCreate(db: SQLiteDatabase): Unit = {}
      override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {}
    }

    def apply(_ctx:Context, _dbName:String, _dbVersion:Int, _dbCreateQuery:String):ObservableDBHelper = new ConcreteSQLiteOpenHelper(_ctx, _dbName, _dbVersion) with ObservableDBHelper{
      override val ctx = _ctx
      override val dbCreateQuery = _dbCreateQuery
    }
  }


}

