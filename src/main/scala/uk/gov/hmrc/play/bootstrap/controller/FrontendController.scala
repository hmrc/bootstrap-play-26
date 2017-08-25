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

package uk.gov.hmrc.play.bootstrap.controller

import org.slf4j.MDC
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionKeys}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent._

trait FrontendController extends BaseController with Utf8MimeTypes {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers, Some(rh.session))

  implicit def mdcExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext = MdcLoggingExecutionContext.fromLoggingDetails

  implicit class SessionKeyRemover(result: Future[Result]) {
    def removeSessionKey(key: String)(implicit request: Request[_]) = result.map {_.withSession(request.session - key)}
  }

}

object UnauthorisedAction {
  def apply(body: (Request[AnyContent] => Result), sensitiveDataFormKeys: Seq[String] = Seq.empty): Action[AnyContent] = unauthedAction(ActionWithMdc(body), sensitiveDataFormKeys)

  def async(body: (Request[AnyContent] => Future[Result]), sensitiveDataFormKeys: Seq[String] = Seq.empty): Action[AnyContent] = unauthedAction(Action.async(body), sensitiveDataFormKeys)

  private def unauthedAction(body: Action[AnyContent], sensitiveDataFormKeys: Seq[String]): Action[AnyContent] = body
}

/**
  * Use this Action with your endpoints, if they are synchronous and require
  * the header carrier values to be logged.
  *
  * For .async actions the MdcLoggingExecutionContext takes care of it.
  */
object ActionWithMdc extends ActionBuilder[Request] {

  private def storeHeaders(request: RequestHeader) {
    request.session.get(SessionKeys.userId).foreach(MDC.put(HeaderNames.authorisation, _))
    request.session.get(SessionKeys.token).foreach(MDC.put(HeaderNames.token, _))
    request.session.get(SessionKeys.sessionId).foreach(MDC.put(HeaderNames.xSessionId, _))
    request.headers.get(HeaderNames.xForwardedFor).foreach(MDC.put(HeaderNames.xForwardedFor, _))
    request.headers.get(HeaderNames.xRequestId).foreach(MDC.put(HeaderNames.xRequestId, _))
    Logger.debug("Request details added to MDC")
  }

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    Logger.debug("Invoke block, setting up MDC due to Action creation")
    storeHeaders(request)
    val r = block(request)
    Logger.debug("Clearing MDC")
    MDC.clear()
    r
  }

}
