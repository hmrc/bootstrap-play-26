/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent._

abstract class FrontendController(mcc: MessagesControllerComponents)
    extends MessagesBaseController
    with Utf8MimeTypes
    with MdcExecutionContextProvider
    with WithJsonBody
    with FrontendHeaderCarrierProvider
    with UnauthorisedActions

trait FrontendHeaderCarrierProvider {
  implicit protected def hc(implicit request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
}

trait UnauthorisedActions { self: MessagesBaseController with FrontendHeaderCarrierProvider =>

  // todo (konrad): should we deprecate one of the below as they do the same thing?

  /**
    * Use this Action with your endpoints, if they are synchronous and require
    * the header carrier values to be logged.
    *
    * For .async actions the MdcLoggingExecutionContext takes care of it.
    */
  def ActionWithMdc: ActionBuilder[MessagesRequest, AnyContent] =
    controllerComponents.messagesActionBuilder.andThen(actionWithMdc)

  def UnauthorizedAction: ActionBuilder[MessagesRequest, AnyContent] =
    ActionWithMdc

  private val actionWithMdc =
    new ActionFunction[MessagesRequest, MessagesRequest] {

      private def storeHeaders(request: RequestHeader) {
        hc(request).mdcData.foreach {
          case (k, v) => MDC.put(k, v)
        }
        Logger.debug("Request details added to MDC")
      }

      def invokeBlock[A](request: MessagesRequest[A], block: MessagesRequest[A] => Future[Result]): Future[Result] = {
        Logger.debug("Invoke block, setting up MDC due to Action creation")
        storeHeaders(request)
        val r = block(request)
        Logger.debug("Clearing MDC")
        MDC.clear()
        r
      }

      val parser: BodyParser[AnyContent] = parse.defaultBodyParser

      protected val executionContext: ExecutionContext = defaultExecutionContext

    }

}
