package forex.helper
import forex.domain.Currency.{ CAD, CHF }
import forex.domain.oneframe.RateDTO
import forex.domain.{ Currency, Price, Rate, Timestamp }

import java.time.{ OffsetDateTime, ZoneOffset }

trait MockedObject {

  val mockedConfig: String = """app {
                                                |  server {
                                                |    host = "localhost"
                                                |    port = 8080
                                                |    request-timeout = 40 seconds
                                                |  }
                                                |  client {
                                                |    one-frame {
                                                |      host = "localhost"
                                                |      port = 8090
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
    pair = Rate.Pair(CHF, CAD),
    price = Price(BigDecimal(1.2)),
    timestamp = mockedTimestamp
  )
}
