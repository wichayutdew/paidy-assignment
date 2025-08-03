package forex.domain.oneframe

object Constant {
  object HEADER {
    val TOKEN: String = "token"
  }
  object PATH {
    val RATES: String = "rates"
  }
  object QUERY_PARAMETER {
    val PAIR: String = "pair"
  }

  object MESSAGE {
    val RATE_LIMIT    = "Quota reached"
    val FAILED_DECODE = "decoding failure"
  }
}
