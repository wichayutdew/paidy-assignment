package forex.services.rates.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.rates.{ Pair, Price, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pairs: List[Pair], token: String): F[Error Either List[Rate]] =
    pairs
      .map(pair => Rate(pair, Price(BigDecimal(100)), Timestamp.now))
      .asRight[Error]
      .pure[F]

}
