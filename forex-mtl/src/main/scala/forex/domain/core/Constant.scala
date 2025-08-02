package forex.domain.core

object Constant {
  object PATH {
    val RATES: String        = "/rates"
    val HEALTH_CHECK: String = "/health"
    val CACHE: String        = "/cache"
    val REDIS: String        = "/redis"
    val MEMORY: String       = "/memory"
  }
  object QUERY_PARAMETER {
    val FROM: String = "from"
    val TO: String   = "to"
  }
}
