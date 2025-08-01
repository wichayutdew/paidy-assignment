package forex.services.rates

import forex.domain.rates.{ Pair, Rate }
import forex.services.rates.errors._

trait Algebra[F[_]] {
  def get(pairs: List[Pair], token: String): F[Error Either List[Rate]]
}
