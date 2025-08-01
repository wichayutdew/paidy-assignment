package forex.config.models

import scala.concurrent.duration.FiniteDuration

final case class ClientSetting(oneFrame: OneFrameConfig, vault: VaultConfig, redis: RedisConfig)

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

final case class RedisConfig(
    host: String,
    port: Int,
    connectionTimeout: FiniteDuration,
    token: String
) extends GenericHttpConfig
