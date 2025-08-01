package forex.domain.rates

import forex.domain.oneframe.RateDTO
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class Rate(
    pair: Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  def fromOneFrameDTO(dto: RateDTO): Rate =
    Rate(
      pair = Pair(dto.from, dto.to),
      price = Price(dto.price),
      timestamp = dto.timestamp
    )


  implicit val rateDecoder: Decoder[Rate] = deriveDecoder[Rate]
  implicit val rateEncoder: Encoder[Rate] = deriveEncoder[Rate]
}
