package forex.domain

import cats.Show
import enumeratum.{ Enum, EnumEntry }
import forex.domain.core.BaseError

sealed trait Currency extends EnumEntry
object Currency extends Enum[Currency] {
  val values: IndexedSeq[Currency] = findValues

  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  val supportedCurrencies: List[String] = values.map(_.entryName).toList

  implicit val show: Show[Currency] = Show.show(_.entryName)

  def fromString(s: String): Either[CurrencyError, Currency] = s.toUpperCase match {
    case str if str.isBlank => Left(CurrencyError.Empty)
    case str                => withNameOption(str).map(Right(_)).getOrElse(Left(CurrencyError.Unsupported(str)))
  }
}

sealed trait CurrencyError extends BaseError
object CurrencyError {
  case object Empty extends CurrencyError
  case class Unsupported(currency: String) extends CurrencyError
}
