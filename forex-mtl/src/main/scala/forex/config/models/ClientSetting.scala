package forex.config.models

import scala.concurrent.duration.FiniteDuration

final case class ClientSetting(oneFrame: OneFrameConfig)

final case class OneFrameConfig(
    host: String,
    port: Int,
    requestTimeout: FiniteDuration,
    connectionTimeout: FiniteDuration,
    token: String
) extends GenericHttpConfig
