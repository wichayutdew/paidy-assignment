package forex.config.models

final case class ApplicationConfig(
    server: ServerSetting,
    client: ClientSetting,
    cache: CacheSetting
)
