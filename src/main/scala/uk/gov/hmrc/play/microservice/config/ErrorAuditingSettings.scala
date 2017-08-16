///*
// * Copyright 2017 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.play.microservice.config
//
//import play.api.http.HttpErrorHandler
//import play.api.mvc.{RequestHeader, Result}
//import uk.gov.hmrc.http.{JsValidationException, NotFoundException}
//import uk.gov.hmrc.play.HeaderCarrierConverter
//import uk.gov.hmrc.play.audit.EventTypes._
//import uk.gov.hmrc.play.audit.http.connector.AuditConnector
//
//import scala.concurrent.Future
//
//trait ErrorAuditingSettings extends HttpAuditEvent with HttpErrorHandler {
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  def auditConnector: AuditConnector
//
//  private val unexpectedError = "Unexpected error"
//  private val notFoundError = "Resource Endpoint Not Found"
//  private val badRequestError = "Request bad format exception"
//
//  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
//    val code = ex match {
//      case e: NotFoundException => ResourceNotFound
//      case jsError: JsValidationException => ServerValidationError
//      case _ => ServerInternalError
//    }
//
//    auditConnector.sendEvent(dataEvent(code, unexpectedError, request, Map(TransactionFailureReason -> ex.getMessage))(HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
//    super.onServerError(request, ex)
//  }
//
//
//  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
//    auditConnector.sendEvent(dataEvent(ResourceNotFound, notFoundError, request)(HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
//    super.onHandlerNotFound(request)
//  }
//
//  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
//    auditConnector.sendEvent(dataEvent(ServerValidationError, badRequestError, request)(HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
//    super.onBadRequest(request, error)
//  }
//}
