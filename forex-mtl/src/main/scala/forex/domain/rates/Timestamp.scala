package forex.domain.rates

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{ Decoder, Encoder }

import java.time.OffsetDateTime

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val timestampEncoder: Encoder[Timestamp] = deriveEncoder[Timestamp]
  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    try Right(Timestamp(OffsetDateTime.parse(str)))
    catch { case _: Exception => Left(s"Failed to parse date: $str") }
  }
}
