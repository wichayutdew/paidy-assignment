package forex.domain.core.measurement.logging

import com.typesafe.scalalogging.{ LazyLogging, Logger }
import io.netty.handler.logging.LogLevel
import org.slf4j.MDC

trait AppLogger extends LazyLogging {
  lazy val className: String = getClass.getName

  implicit class LoggerLike(val logger: Logger) {
    def log(logMsg: BaseLog): Unit = {
      val logWithLevel = (logMsg.logLevel, logMsg.cause) match {
        case (LogLevel.TRACE, None)     => logger.trace(_: String)
        case (LogLevel.TRACE, Some(ex)) => logger.trace(_: String, ex)
        case (LogLevel.DEBUG, None)     => logger.debug(_: String)
        case (LogLevel.DEBUG, Some(ex)) => logger.debug(_: String, ex)
        case (LogLevel.INFO, None)      => logger.info(_: String)
        case (LogLevel.INFO, Some(ex))  => logger.info(_: String, ex)
        case (LogLevel.WARN, None)      => logger.warn(_: String)
        case (LogLevel.WARN, Some(ex))  => logger.warn(_: String, ex)
        case (LogLevel.ERROR, None)     => logger.error(_: String)
        case (LogLevel.ERROR, Some(ex)) => logger.error(_: String, ex)
      }

      logWithLevel(logMsg.message)
      MDC.clear()
    }
  }

}
