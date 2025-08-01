package forex.domain.rates
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

final case class Price(value: BigDecimal) extends AnyVal

object Price {
  def apply(value: Int): Price = Price(BigDecimal(value))

  implicit val priceDecoder: Decoder[Price] = deriveDecoder[Price]
  implicit val priceEncoder: Encoder[Price] = deriveEncoder[Price]
}
