package forex.http.rates
import forex.domain.Currency.{ CAD, CHF }
import forex.domain.{ Price, Rate, Timestamp }
import forex.http.rates.Converters.GetApiResponseOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{ OffsetDateTime, ZoneOffset }

class ConvertersSpec extends AnyWordSpec with Matchers {
  "Converters" when {
    "asGetApiResponse" should {
      "convert Rate to GetApiResponse" in {
        val timestamp = Timestamp(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.of("Z")))
        val rate      = Rate(
          pair = Rate.Pair(CHF, CAD),
          price = Price(BigDecimal(1.2)),
          timestamp = timestamp
        )

        val result = rate.asGetApiResponse

        result.from shouldBe CHF
        result.to shouldBe CAD
        result.price shouldBe Price(BigDecimal(1.2))
        result.timestamp shouldBe timestamp
      }
    }
  }
}
