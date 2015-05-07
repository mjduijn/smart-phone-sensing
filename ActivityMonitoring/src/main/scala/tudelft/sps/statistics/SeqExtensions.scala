package tudelft.sps.statistics

object SeqExtensions{
  implicit class StatisticalIntSeqExtensions(val seq:Seq[Float]) extends AnyVal {
    def mean = seq.sum / seq.length
    def variance = {
      if(seq.isEmpty) {
        0
      } else {
        val m = mean
        Math.sqrt(seq.reduceRight[Float]((cur, acc) => Math.pow(mean - cur, 2).asInstanceOf[Float] + acc))
      }
    }
  }
}