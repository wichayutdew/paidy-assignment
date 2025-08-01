package forex

//$COVERAGE-OFF$
package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters
  final val VaultServices = vault.Interpreters
}
//$COVERAGE-ON$
