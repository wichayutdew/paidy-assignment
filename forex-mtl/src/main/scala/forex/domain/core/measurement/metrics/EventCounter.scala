package forex.domain.core.measurement.metrics

import forex.domain.core.measurement.logging.{AppLogger, InfoLog}
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{LongCounter, Meter}

case class EventCounter(counter: LongCounter, client: String) extends AppLogger {
  def record(metricsTags: Map[String, String] = Map.empty): Unit = {
    val attributes = buildAttributes(metricsTags)
    logger.log(InfoLog(s"Recording event"))
    counter.add(1L, attributes)
  }

  private def buildAttributes(additionalAttributes: Map[String, String]): Attributes = {
    val builder = Attributes
      .builder()
      .put(MetricsTag.CLIENT, client)
    additionalAttributes.foreach { case (key, value) => builder.put(key, value) }
    builder.build()
  }
}

object EventCounter {
  def apply(name: String, client: String)(implicit meter: Meter): EventCounter = new EventCounter(
    meter
      .counterBuilder(name)
      .setDescription(s"Total count of $name events")
      .build(),
    client
  )
}
