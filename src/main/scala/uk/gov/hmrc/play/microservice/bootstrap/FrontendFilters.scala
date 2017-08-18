package uk.gov.hmrc.play.microservice.bootstrap

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.MetricsFilter
import play.api.Configuration
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import play.api.http.HttpFilters
import uk.gov.hmrc.play.http.logging.filters._
import uk.gov.hmrc.play.microservice.filters.CacheControlFilter
import uk.gov.hmrc.play.microservice.filters.frontend._

@Singleton
class FrontendFilters @Inject()
(
  configuration: Configuration,
  loggingFilter: FrontendLoggingFilter,
  headersFilter: HeadersFilter,
  securityFilter: SecurityHeadersFilter,
  frontendAuditFilter: FrontendAuditFilter,
  metricsFilter: MetricsFilter,
  deviceIdFilter: DeviceIdFilter,
  csrfFilter: CSRFFilter,
  sessionTimeoutFilter: SessionTimeoutFilter,
  csrfExceptionsFilter: CSRFExceptionsFilter,
  cacheControlFilter: CacheControlFilter
) extends HttpFilters {

  val frontendFilters = Seq(
    metricsFilter,
    headersFilter,
    SessionCookieCryptoFilter,
    deviceIdFilter,
    loggingFilter,
    frontendAuditFilter,
    sessionTimeoutFilter,
    csrfExceptionsFilter,
    csrfFilter,
    cacheControlFilter)

  lazy val enableSecurityHeaderFilter: Boolean = configuration.getBoolean("security.headers.filter.enabled").getOrElse(true)

  override val filters =
    if (enableSecurityHeaderFilter) Seq(securityFilter) ++ frontendFilters else frontendFilters

}
