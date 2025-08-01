package forex.domain.rates
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

final case class Pair(
    from: Currency,
    to: Currency
) {
  def toCacheKey: String = s"${from.entryName}${to.entryName}"
}

object Pair {
  implicit val pairDecoder: Decoder[Pair] = deriveDecoder[Pair]
  implicit val pairEncoder: Encoder[Pair] = deriveEncoder[Pair]
}
