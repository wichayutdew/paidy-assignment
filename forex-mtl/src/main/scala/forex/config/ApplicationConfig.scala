package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(server: HttpConfig, client: ClientSettings)

case class ClientSettings(oneFrame: HttpConfig)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)
