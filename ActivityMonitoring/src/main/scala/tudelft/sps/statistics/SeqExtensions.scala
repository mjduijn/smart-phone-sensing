package tudelft.sps.statistics

object SeqExtensions{
  implicit class StatisticalSeqExtensions[A](seq:Seq[A])(implicit ev:Numeric[A]) {
    def mean:Double = ev.toDouble(seq.sum) / seq.size
    def variance:Double = {
      if(seq.isEmpty) {
        0
      } else {
        val m = mean
        seq.foldRight[Double](0)((cur, acc) => Math.pow(mean - ev.toDouble(cur), 2) + acc) / seq.size
      }
    }
    def stdev:Double = Math.sqrt(variance)
    def median:A = seq.sorted.apply(seq.size / 2)
  }
}