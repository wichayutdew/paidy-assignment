package forex.config.models
import scala.concurrent.duration.FiniteDuration

final case class CacheSetting(
    rates: CacheConfig
)

final case class CacheConfig(
    enabled: Boolean,
    prefix: String,
    ttl: FiniteDuration
)
