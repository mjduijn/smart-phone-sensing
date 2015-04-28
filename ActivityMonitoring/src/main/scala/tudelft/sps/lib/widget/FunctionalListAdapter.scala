package tudelft.sps.lib.widget

import android.content.Context
import android.util.Log
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.BaseAdapter

trait FunctionalListAdapter[A <: AnyRef, B] extends BaseAdapter{
  protected var seq:scala.collection.mutable.IndexedSeq[A]
  val ctx:Context
  val layoutId:Int
  val viewHolderBuilder: (View) => B
  val viewBuilder: (B, A) => Unit

  private lazy val layoutInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  override def getCount: Int = seq.size

  override def getItemId(id: Int): Long = id

  override def getView(index: Int, convertView: View, parent: ViewGroup): View = {
    if(convertView == null){
      val view = layoutInflater.inflate(layoutId, parent, false)
      val viewHolder = viewHolderBuilder(view)
      view.setTag(viewHolder)
      viewBuilder(viewHolder, seq(index))
      return view
    } else{
      val viewHolder = convertView.getTag().asInstanceOf[B]
      viewBuilder(viewHolder, seq(index))
      return convertView
    }

  }

  override def getItem(id: Int): AnyRef = seq(id)
}

object FunctionalListAdapter{
  def apply[A <: AnyRef, B](_seq:scala.collection.mutable.IndexedSeq[A], _ctx:Context, _layoutId:Int)(_viewHolderBuilder: View => B)(_viewBuilder: (B, A) => Unit) = new FunctionalListAdapter[A, B] {
    override var seq = _seq
    override val ctx = _ctx
    override val layoutId = _layoutId
    override val viewHolderBuilder = _viewHolderBuilder
    override val viewBuilder = _viewBuilder
  }
}
