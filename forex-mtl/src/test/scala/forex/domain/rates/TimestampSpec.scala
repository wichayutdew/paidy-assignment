package forex.domain.rates

import forex.helper.MockedObject
import io.circe.DecodingFailure
import io.circe.parser.decode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TimestampSpec extends AnyWordSpec with Matchers with MockedObject {
  "Timestamp" when {
    "decode" should {
      "decode valid timestamp strings" in {
        decode[Timestamp](s"\"${mockedTimestamp.value.toString}\"") shouldBe Right(mockedTimestamp)
      }

      "fail to decode invalid timestamp strings" in {
        decode[Timestamp]("\"invalid-date\"") shouldBe Left(
          DecodingFailure("Failed to parse date: invalid-date", List())
        )
      }
    }
  }
}
