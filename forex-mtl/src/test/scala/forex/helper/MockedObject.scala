package forex.helper
import forex.config.models.{ ApplicationConfig, ClientSetting, OneFrameConfig, ServerSetting }
import forex.domain.oneframe.RateDTO
import forex.domain.rates._

import java.time.{ OffsetDateTime, ZoneOffset }
import scala.concurrent.duration.{ FiniteDuration, SECONDS }

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
                                                |      token = "test-token"
                                                |    }
                                                |  }
                                                |}""".stripMargin

  val mockedTimestamp: Timestamp = Timestamp(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.of("Z")))

  val mockedRateDTO: RateDTO = RateDTO(
    from = Currency.USD,
    to = Currency.EUR,
    bid = BigDecimal(1.2),
    ask = BigDecimal(1.3),
    price = BigDecimal(1.2),
    timestamp = mockedTimestamp
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
    client = ClientSetting(oneFrame =
      OneFrameConfig(
        host = "localhost",
        port = 8080,
        requestTimeout = FiniteDuration(30, SECONDS),
        connectionTimeout = FiniteDuration(30, SECONDS),
        token = "test-token"
      )
    )
  )
}
