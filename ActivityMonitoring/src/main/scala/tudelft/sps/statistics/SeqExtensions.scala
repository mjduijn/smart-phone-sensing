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

  object SeqMath{
    def mean(seq:Seq[Double], m:Int, n:Int):Double = {
      var i = m
      var sum:Double = 0
      while(i < n){
        sum = sum + seq(i)
        i = i + i
      }
      sum / (n - m)
    }

    def variance(seq:Seq[Double], m:Int, n:Int):Double = {
      if(seq.isEmpty){
        return 0
      } else{
        val theMean = mean(seq, m, n)
        var i = m
        var sum:Double = 0
        while(i < n){
          sum = sum + Math.pow(theMean - seq(i), 2)
          i = i + 1
        }
        sum / (n - m)
      }
    }
    def stdev(seq:Seq[Double], m:Int, n:Int):Double = Math.sqrt(variance(seq, m, n))
  }
}