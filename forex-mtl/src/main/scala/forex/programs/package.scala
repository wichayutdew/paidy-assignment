package forex

//$COVERAGE-OFF$
package object programs {
  type RatesProgram[F[_]] = rates.Algebra[F]
  final val RatesProgram = rates.Program

  type CacheProgram[F[_]] = cache.Algebra[F]
  final val CacheProgram = cache.Program
}
//$COVERAGE-ON$
