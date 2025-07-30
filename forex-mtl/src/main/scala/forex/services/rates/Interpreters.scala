package forex.services.rates

import cats.Applicative
import interpreters._

//$COVERAGE-OFF$
object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
}
//$COVERAGE-ON$
