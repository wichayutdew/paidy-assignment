package forex.domain.rates

import forex.helper.MockedObject
import io.circe.DecodingFailure
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TimestampSpec extends AnyWordSpec with Matchers with MockedObject {
  "Timestamp" when {
    "encode" should {
      "encode valid timestamp" in {
        mockedTimestamp.asJson.noSpaces shouldBe mockedTimestampObject
      }
    }
    "decode" should {
      "decode valid timestamp strings" in {
        decode[Timestamp](mockedTimestampObject) shouldBe Right(mockedTimestamp)
      }

      "fail to decode invalid timestamp strings" in {
        val decoded = decode[Timestamp]("\"invalid-date\"")
        decoded.isLeft shouldBe true
        decoded.left.toOption.get shouldBe a[DecodingFailure]
      }
    }
  }
}
