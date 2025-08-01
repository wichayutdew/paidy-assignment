package forex.config.models

import scala.concurrent.duration.FiniteDuration

final case class ClientSetting(oneFrame: OneFrameConfig, vault: VaultConfig)

final case class OneFrameConfig(
    host: String,
    port: Int,
    requestTimeout: FiniteDuration,
    connectionTimeout: FiniteDuration
) extends GenericHttpConfig

final case class VaultConfig(
    host: String,
    port: Int,
    requestTimeout: FiniteDuration,
    connectionTimeout: FiniteDuration,
    token: String
) extends GenericHttpConfig
