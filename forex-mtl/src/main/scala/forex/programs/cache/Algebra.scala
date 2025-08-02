package forex.programs.cache

trait Algebra[F[_]] {
  def delete(key: String, service: String): F[Unit]
}
