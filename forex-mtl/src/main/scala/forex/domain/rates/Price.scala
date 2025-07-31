package forex.domain.rates

final case class Price(value: BigDecimal) extends AnyVal

object Price {
  def apply(value: Int): Price = Price(BigDecimal(value))
}
