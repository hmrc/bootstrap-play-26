package uk.gov.hmrc.play.microservice.bootstrap

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.MetricsFilter
import play.api.Configuration
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.play.http.logging.filters._
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, CacheControlFilter}

@Singleton
class BaseFilters @Inject()
(
  configuration: Configuration,
  metricsFilter: MetricsFilter,
  auditFilter: AuditFilter,
  loggingFilter: LoggingFilter,
  cacheFilter: CacheControlFilter
) extends HttpFilters {

  override val filters: Seq[EssentialFilter] = Seq(
    metricsFilter,
    auditFilter,
    loggingFilter,
    cacheFilter)
}
