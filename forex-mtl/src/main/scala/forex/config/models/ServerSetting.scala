package forex.config.models

import scala.concurrent.duration.FiniteDuration

final case class ServerSetting(
    host: String,
    port: Int,
    requestTimeout: FiniteDuration
) extends GenericHttpConfig
