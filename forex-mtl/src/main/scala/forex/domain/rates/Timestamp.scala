package forex.domain.rates

import io.circe.{ Decoder, Encoder, HCursor, Json }

import java.time.OffsetDateTime

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val timestampEncoder: Encoder[Timestamp] = Encoder.instance { ts =>
    Json.obj("value" -> Encoder.encodeString(ts.value.toString))
  }
  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.instance { c: HCursor =>
    c.downField("value").as[String].map(str => Timestamp(OffsetDateTime.parse(str)))
  }
}
