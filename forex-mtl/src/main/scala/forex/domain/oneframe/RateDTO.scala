package forex.domain.oneframe
import forex.domain.rates.Currency
import io.circe.{ Decoder, HCursor }

import java.time.OffsetDateTime

final case class RateDTO(
    from: Currency,
    to: Currency,
    bid: BigDecimal,
    ask: BigDecimal,
    price: BigDecimal,
    timestamp: OffsetDateTime
)

object RateDTO {
  implicit val rateDTODecoder: Decoder[RateDTO] = (c: HCursor) =>
    for {
      from <- c.downField("from").as[Currency]
      to <- c.downField("to").as[Currency]
      bid <- c.downField("bid").as[BigDecimal]
      ask <- c.downField("ask").as[BigDecimal]
      price <- c.downField("price").as[BigDecimal]
      timestamp <- c.downField("time_stamp").as[OffsetDateTime]
    } yield RateDTO(from, to, bid, ask, price, timestamp)
}
