package forex.programs.rates

import forex.domain.rates.Currency

object Protocol {
  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  )
}
