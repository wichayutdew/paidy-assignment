package forex.http.rates
import forex.domain.Currency
import org.http4s.QueryParameterValue
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class QueryParamsSpec extends AnyWordSpec with Matchers {
  "QueryParams" when {
    "decode" should {
      "successfully decode valid currencies" in {
        val result = QueryParams.currencyQueryParam.decode(QueryParameterValue("EUR"))
        result.isValid shouldBe true
        result.toOption.get shouldBe Currency.EUR
      }

      "fail to decode unsupported currency" in {
        val result = QueryParams.currencyQueryParam.decode(QueryParameterValue("XYZ"))
        result.isInvalid shouldBe true

        val failure = result.swap.toOption.get.head
        failure.sanitized shouldBe "Invalid currency: XYZ"
        failure.details shouldBe "List of supported currencies: AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD"
      }

      "fail to decode empty field" in {
        val result = QueryParams.currencyQueryParam.decode(QueryParameterValue(""))
        result.isInvalid shouldBe true

        val failure = result.swap.toOption.get.head
        failure.sanitized shouldBe "Empty currency"
        failure.details shouldBe "Currency parameter should not be empty"
      }

      "fail to decode blank field" in {
        val result = QueryParams.currencyQueryParam.decode(QueryParameterValue(" "))
        result.isInvalid shouldBe true

        val failure = result.swap.toOption.get.head
        failure.sanitized shouldBe "Empty currency"
        failure.details shouldBe "Currency parameter should not be empty"
      }
    }
  }
}
