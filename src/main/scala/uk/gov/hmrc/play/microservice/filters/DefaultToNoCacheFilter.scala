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

package uk.gov.hmrc.play.microservice.filters

import play.api.mvc.{Filter, RequestHeader, Result}
import play.mvc.Http.HeaderNames

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This filter adds Cache-Control: no-cache,no-store,max-age=0 headers
  * to any responses that do not already have a Cache-Control header.
  */
object DefaultToNoCacheFilter extends Filter with MicroserviceFilterSupport {

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    next(rh).map { r =>
      r.header.headers.get(HeaderNames.CACHE_CONTROL) match {
        case Some(_) => r
        case _ => r.withHeaders(CommonHeaders.NoCacheHeader)
      }
    }
  }
}
