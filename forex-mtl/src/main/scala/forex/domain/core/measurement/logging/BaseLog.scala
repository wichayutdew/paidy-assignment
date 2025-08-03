package forex.domain.core.measurement.logging
import io.netty.handler.logging.LogLevel

trait BaseLog {
  def logLevel: LogLevel
  def message: String
  def cause: Option[Throwable] = None
}

final case class DebugLog(
    override val message: String
) extends BaseLog {
  override val logLevel: LogLevel = LogLevel.DEBUG
}

final case class InfoLog(
    override val message: String
) extends BaseLog {
  override val logLevel: LogLevel = LogLevel.INFO
}

final case class WarnLog(
    override val message: String
) extends BaseLog {
  override val logLevel: LogLevel = LogLevel.WARN
}

final case class ErrorLog(
    override val message: String,
    override val cause: Option[Throwable]
) extends BaseLog {
  override val logLevel: LogLevel = LogLevel.ERROR
}
