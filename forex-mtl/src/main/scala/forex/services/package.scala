package forex

//$COVERAGE-OFF$
package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type SecretManagerService[F[_]] = secretManager.Algebra[F]
  final val SecretManagerServices = secretManager.Interpreters

  type ExternalCacheService[F[_]] = externalCache.Algebra[F]
  final val ExternalCacheServices = externalCache.Interpreters
}
//$COVERAGE-ON$
