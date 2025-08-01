package forex

//$COVERAGE-OFF$
package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type SecretManagerService[F[_]] = secretManager.Algebra[F]
  final val SecretManagerServices = secretManager.Interpreters

  type CacheService[F[_]] = cache.Algebra[F]
  final val CacheServices = cache.Interpreters
}
//$COVERAGE-ON$
