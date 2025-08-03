package forex.domain.core.measurement.metrics
import forex.domain.core.measurement.logging.{ AppLogger, DebugLog }
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{ DoubleHistogram, Meter }

case class TimerMetric(histogram: DoubleHistogram, client: String) extends AppLogger {

  def recordDuration(durationSeconds: Double, metricsTags: Map[String, String] = Map.empty): Unit = {
    val attributes = buildAttributes(metricsTags)
    logger.log(DebugLog(s"Recording execution time: ${durationSeconds} s"))
    histogram.record(durationSeconds, attributes)
  }

  private[metrics] def buildAttributes(additionalAttributes: Map[String, String]): Attributes = {
    val builder = Attributes
      .builder()
      .put(MetricsTag.CLIENT, client)
    additionalAttributes.foreach { case (key, value) => builder.put(key, value) }
    builder.build()
  }
}

object TimerMetric {
  def apply(name: String, client: String)(implicit meter: Meter): TimerMetric = new TimerMetric(
    meter
      .histogramBuilder(name)
      .setDescription(s"Execution time for $name")
      .setUnit("ms")
      .build(),
    client
  )
}
