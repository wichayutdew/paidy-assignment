package forex.programs.rates

import errors._
import forex.domain.rates.Rate

trait Algebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[Error Either Rate]
  def preFetch(): F[Unit]
}
