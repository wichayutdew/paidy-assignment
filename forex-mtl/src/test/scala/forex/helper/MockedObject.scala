package forex.helper
import forex.config.models._
import forex.domain.oneframe.RateDTO
import forex.domain.rates._

import java.time.{ OffsetDateTime, ZoneOffset }
import scala.concurrent.duration.{ FiniteDuration, HOURS, MINUTES, SECONDS }

trait MockedObject {

  val mockedConfig: String = """app {
                                                |  server {
                                                |    host = "localhost"
                                                |    port = 8090
                                                |    request-timeout = 40 seconds
                                                |  }
                                                |  client {
                                                |    one-frame {
                                                |      host = "localhost"
                                                |      port = 8080
                                                |      request-timeout = 30 seconds
                                                |      connection-timeout = 30 seconds
                                                |    }
                                                |    vault {
                                                |      host = "localhost"
                                                |      port = 8200
                                                |      request-timeout = 30 seconds
                                                |      connection-timeout = 30 seconds
                                                |      token = "test-token"
                                                |    }
                                                |    redis {
                                                |      host = "localhost"
                                                |      port = 6379
                                                |      connection-timeout = 30 seconds
                                                |      token = "test-token"
                                                |    }
                                                |  }
                                                |  cache {
                                                |    rates {
                                                |      enabled = true
                                                |      prefix = "rates"
                                                |      ttl = 5 minutes
                                                |    }
                                                |    token {
                                                |      enabled = true
                                                |      prefix = "token"
                                                |      ttl = 3 hours
                                                |    }
                                                |  }
                                                |}""".stripMargin

  val mockedTimestampObject      = """{"value":"2099-12-31T23:59:59Z"}"""
  val mockedTimestamp: Timestamp = Timestamp(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.of("Z")))

  val mockedRateDTO: RateDTO = RateDTO(
    from = Currency.USD,
    to = Currency.EUR,
    bid = BigDecimal(1.2),
    ask = BigDecimal(1.3),
    price = BigDecimal(1.2),
    timestamp = mockedTimestamp.value
  )

  val mockedRate: Rate = Rate(
    pair = Pair(Currency.CHF, Currency.CAD),
    price = Price(BigDecimal(1.2)),
    timestamp = mockedTimestamp
  )

  val mockedApplicationConfig: ApplicationConfig = ApplicationConfig(
    server = ServerSetting(
      host = "localhost",
      port = 8090,
      requestTimeout = FiniteDuration(40, SECONDS)
    ),
    client = ClientSetting(
      oneFrame = OneFrameConfig(
        host = "localhost",
        port = 8080,
        requestTimeout = FiniteDuration(30, SECONDS),
        connectionTimeout = FiniteDuration(30, SECONDS)
      ),
      vault = VaultConfig(
        host = "localhost",
        port = 8200,
        requestTimeout = FiniteDuration(30, SECONDS),
        connectionTimeout = FiniteDuration(30, SECONDS),
        token = "test-token"
      ),
      redis = RedisConfig(
        host = "localhost",
        port = 6379,
        connectionTimeout = FiniteDuration(30, SECONDS),
        token = "test-token"
      )
    ),
    cache = CacheSetting(
      rates = CacheConfig(
        enabled = true,
        prefix = "rates",
        ttl = FiniteDuration(5, MINUTES)
      ),
      token = CacheConfig(enabled = true, prefix = "token", ttl = FiniteDuration(3, HOURS))
    )
  )

  val mockedToken = "test-token"
}
