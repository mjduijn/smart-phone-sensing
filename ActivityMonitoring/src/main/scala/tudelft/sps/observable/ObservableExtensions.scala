package tudelft.sps

import rx.lang.scala.Observable

package object observable{
  implicit class ObservableExtensions[A](val observable:Observable[A]){
    /**
     * similar to [[Observable.slidingBuffer(n, 1))]], but starts emitting already before the buffer is full
     * @param n
     * @param skip
     */
    def slider(n:Int):Observable[Seq[A]] = {
      observable.take(n - 1).scan(Seq[A]()){ (acc, cur) =>
        acc :+ cur
      }.merge(observable.slidingBuffer(n, 1))
    }
  }
}