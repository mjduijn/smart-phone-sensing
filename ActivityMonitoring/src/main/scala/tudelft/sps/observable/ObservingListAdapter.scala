package tudelft.sps.observable

import android.content.Context
import android.view.View
import tudelft.sps.lib.widget.FunctionalListAdapter

trait ObservingListAdapter[A <: AnyRef, B] extends FunctionalListAdapter[A,B]{

  override type SequenceType = Seq[A]
  /**
   * Observer method for this adapter. Whenever the data structure changes, the ListView gets
   * updated. Must be called from the UI THread
   */
  def onNext[Sub <: Seq[A]]: Sub => Unit = { next =>
    seq = next.asInstanceOf[SequenceType]
    notifyDataSetChanged()
  }
}

object ObservingListAdapter{
  def apply[A <: AnyRef, B](_ctx:Context, _layoutId:Int)(_viewHolderBuilder: View => B)(_viewBuilder: (B, A) => Unit): ObservingListAdapter[A,B] = new ObservingListAdapter[A,B] {
    override val ctx = _ctx
    override var seq = Seq[A]()
    override val layoutId = _layoutId
    override val viewHolderBuilder = _viewHolderBuilder
    override val viewBuilder = _viewBuilder
  }
}