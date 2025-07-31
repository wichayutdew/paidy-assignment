package forex.domain.rates

import org.scalatest.wordspec.AnyWordSpec

class PriceSpec extends AnyWordSpec {
  "Price" should {
    "be created from an Integer" in {
      val price = Price(100)
      assert(price.value == BigDecimal(100))
    }

    "be created from a BigDecimal" in {
      val price = Price(BigDecimal(99.99))
      assert(price.value == BigDecimal(99.99))
    }
  }
}
