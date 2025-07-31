package forex.domain.rates

import io.circe.Decoder

import java.time.OffsetDateTime

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    try Right(Timestamp(OffsetDateTime.parse(str)))
    catch { case _: Exception => Left(s"Failed to parse date: $str") }
  }
}
