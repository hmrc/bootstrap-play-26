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

package uk.gov.hmrc.play.microservice.filters.frontend

import java.util.UUID
import javax.inject.Inject

import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc._
import uk.gov.hmrc.http.HeaderNames.{xRequestId, xRequestTimestamp}

class HeadersFilter @Inject()() extends EssentialFilter {

  def apply(nextAction: EssentialAction): EssentialAction = new EssentialAction {
    def apply(request: RequestHeader): Accumulator[ByteString, Result] = {
      request.session.get(xRequestId) match {
        case Some(s) => nextAction(request)
        case _ => nextAction(addHeaders(request))
      }
    }

    def addHeaders(request: RequestHeader): RequestHeader = {
      val rid = s"govuk-tax-${UUID.randomUUID().toString}"
      val requestIdHeader = xRequestId -> rid
      val requestTimestampHeader = xRequestTimestamp -> System.nanoTime().toString
      val newHeaders = request.headers.add(requestIdHeader, requestTimestampHeader)
      request.copy(headers = newHeaders)
    }
  }
}
