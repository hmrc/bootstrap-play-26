/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.bootstrap.http

import javax.inject.Inject
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.play.bootstrap.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class JsonErrorHandler @Inject()(auditConnector: AuditConnector, httpAuditEvent: HttpAuditEvent)
    extends HttpErrorHandler
    with BackendHeaderCarrierProvider {

  import httpAuditEvent.dataEvent

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful {
      implicit val headerCarrier: HeaderCarrier = hc(request)
      statusCode match {
        case NOT_FOUND =>
          auditConnector.sendEvent(
            dataEvent(
              eventType       = "ResourceNotFound",
              transactionName = "Resource Endpoint Not Found",
              request         = request,
              detail          = Map.empty
            )
          )
          NotFound(toJson(ErrorResponse(NOT_FOUND, "URI not found", requested = Some(request.path))))
        case BAD_REQUEST =>
          auditConnector.sendEvent(
            dataEvent(
              eventType       = "ServerValidationError",
              transactionName = "Request bad format exception",
              request         = request,
              detail          = Map.empty
            )
          )
          BadRequest(toJson(ErrorResponse(BAD_REQUEST, "bad request")))
        case _ =>
          auditConnector.sendEvent(
            dataEvent(
              eventType       = "ClientError",
              transactionName = s"A client error occurred, status: $statusCode",
              request         = request,
              detail          = Map.empty
            )
          )
          Status(statusCode)(toJson(ErrorResponse(statusCode, message)))
      }
    }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {

    implicit val headerCarrier: HeaderCarrier = hc(request)

    Logger.error(s"! Internal server error, for (${request.method}) [${request.uri}] -> ", ex)

    val eventType = ex match {
      case e: NotFoundException      => "ResourceNotFound"
      case e: AuthorisationException => "ClientError"
      case _: JsValidationException  => "ServerValidationError"
      case _                         => "ServerInternalError"
    }

    auditConnector.sendEvent(
      dataEvent(
        eventType       = eventType,
        transactionName = "Unexpected error",
        request         = request,
        detail          = Map("transactionFailureReason" -> ex.getMessage)
      )
    )
    Future.successful(resolveError(ex))
  }

  private def resolveError(ex: Throwable): Result = {
    val errorResponse = ex match {
      case e: AuthorisationException => ErrorResponse(401, e.getMessage)
      case e: HttpException          => ErrorResponse(e.responseCode, e.getMessage)
      case e: Upstream4xxResponse    => ErrorResponse(e.reportAs, e.getMessage)
      case e: Upstream5xxResponse    => ErrorResponse(e.reportAs, e.getMessage)
      case e: Throwable              => ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage)
    }

    new Status(errorResponse.statusCode)(toJson(errorResponse))
  }
}
