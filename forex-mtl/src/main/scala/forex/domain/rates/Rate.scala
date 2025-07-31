package forex.domain.rates

import forex.domain.oneframe.RateDTO

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
}
