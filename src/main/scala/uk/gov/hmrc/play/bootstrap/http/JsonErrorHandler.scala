/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.{Configuration, Logger}
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

case class ErrorResponse(statusCode: Int, message: String, xStatusCode: Option[String] = None, requested: Option[String] = None)

class JsonErrorHandler @Inject()(configuration: Configuration, auditConnector: AuditConnector)
  extends HttpErrorHandler with HttpAuditEvent {

  implicit val erFormats = Json.format[ErrorResponse]

  override val appName: String = configuration.getString("appName").getOrElse("APP NAME NOT SET")

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {

    implicit val headerCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    statusCode match {
      case play.mvc.Http.Status.NOT_FOUND =>
        auditConnector.sendEvent(dataEvent("ResourceNotFound", "Resource Endpoint Not Found", request))
        Future.successful(NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "URI not found", requested = Some(request.path)))))
      case play.mvc.Http.Status.BAD_REQUEST =>
        auditConnector.sendEvent(dataEvent("ServerValidationError", "Request bad format exception", request))
        Future.successful(BadRequest(Json.toJson(ErrorResponse(BAD_REQUEST, "bad request"))))
      case _ =>
        auditConnector.sendEvent(dataEvent("ClientError", s"A client error occurred, status: $statusCode", request))
        Future.successful(Status(statusCode)(Json.toJson(ErrorResponse(statusCode, message))))
    }
  }

  override def onServerError(request: RequestHeader, ex: Throwable) = {
    implicit val headerCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    Logger.error(s"! Internal server error, for (${request.method}) [${request.uri}] -> ", ex)

    val code = ex match {
      case e: NotFoundException => "ResourceNotFound"
      case jsError: JsValidationException => "ServerValidationError"
      case _ => "ServerInternalError"
    }

    auditConnector.sendEvent(dataEvent(code, "Unexpected error", request, Map("transactionFailureReason" -> ex.getMessage)))
    Future.successful(resolveError(ex))
  }

  private def resolveError(ex: Throwable): Result = {
    val errorResponse = ex match {
      case e: HttpException => ErrorResponse(e.responseCode, e.getMessage)
      case e: Upstream4xxResponse => ErrorResponse(e.reportAs, e.getMessage)
      case e: Upstream5xxResponse => ErrorResponse(e.reportAs, e.getMessage)
      case e: Throwable => ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage)
    }

    new Status(errorResponse.statusCode)(Json.toJson(errorResponse))
  }
}
