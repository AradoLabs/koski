package fi.oph.koski.pulssi

import com.typesafe.config.Config
import fi.oph.koski.json.{GenericJsonFormats, Json}
import org.json4s.JValue

object PrometheusRepository {
  def apply(config: Config) = {
    if (config.getString("prometheus.url") == "mock") {
      MockPrometheusRepository
    } else {
      ???
    }
  }
}

trait PrometheusRepository {
  implicit val formats = GenericJsonFormats.genericFormats
  def auditLogMetrics: Map[String, Int] = {
    val json = doQuery("/prometheus/api/v1/query?query=increase(fi_oph_koski_log_AuditLog[30d])")
    (json \ "data" \ "result").extract[List[JValue]].map { metric =>
      val operation = (metric \ "metric" \ "operation").extract[String]
      val count = (metric \ "value").extract[List[String]].lastOption.map(_.toDouble.toInt).getOrElse(0)
      (operation, Math.max(0, count))
    }.toMap
  }

  def doQuery(query: String): JValue
}