package forex.config.models

import scala.concurrent.duration.FiniteDuration

trait GenericHttpConfig {
  def host: String
  def port: Int
  def requestTimeout: FiniteDuration
}
