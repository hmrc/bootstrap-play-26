package uk.gov.hmrc.play.microservice.http

import javax.inject.Inject

import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router

class DefaultRequestHandler @Inject() (router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration, filters: HttpFilters)
  extends DefaultHttpRequestHandler(router, errorHandler, configuration, filters) {

  // Play 2.0 doesn't support trailing slash
  override def routeRequest(request: RequestHeader): Option[Handler] = super.routeRequest(request).orElse {
    Some(request.path).filter(_.endsWith("/")).flatMap(p => super.routeRequest(request.copy(path = p.dropRight(1))))
  }
}
