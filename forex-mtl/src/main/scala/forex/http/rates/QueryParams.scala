package forex.http.rates

import forex.domain.rates.{ Currency, CurrencyError }
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder }

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap {
      Currency.fromString(_) match {
        case Right(currency)                          => Right(currency)
        case Left(CurrencyError.Unsupported(invalid)) =>
          Left(
            ParseFailure(
              s"Invalid currency: $invalid",
              s"List of supported currencies: ${Currency.supportedCurrencies.mkString(", ")}"
            )
          )
        case Left(CurrencyError.Empty) => Left(ParseFailure("Empty currency", "Currency parameter should not be empty"))
      }
    }

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
