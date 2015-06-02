package tudelft.sps

import rx.lang.scala.Observable
import scala.concurrent.duration._

package object observable{
  implicit class ObservableExtensions[A](val observable:Observable[A]) extends AnyVal{
    /**
     * similar to [[Observable.slidingBuffer( n, 1 ) )]], but starts emitting already before the buffer is full
     * @param n
     * @param skip
     */
    def slider(n: Int): Observable[Seq[A]] = {
      slider(n, 1)
    }

    /**
     * similar to [[Observable.slidingBuffer( n, s k i p )]], but starts emitting already before the buffer is full
     * @param n
     * @param skip
     */
    def slider(n: Int, skip: Int): Observable[Seq[A]] = {
      observable.take(n - 1).scan(Seq[A]()) { (acc, cur) =>
        acc :+ cur
      }.merge(observable.slidingBuffer(n, skip))
    }

    def zipWithPrevious: Observable[(A, A)] = observable
      .slidingBuffer(2, 1)
      .map(seq => (seq(0), seq(1)))


    def toggle[A](first:A, second:A):Observable[A] = observable.scan(first){(prev, cur) => if(prev.equals(first)) second else first}
    def toggle():Observable[Boolean] = observable.toggle(true, false)
  }
}