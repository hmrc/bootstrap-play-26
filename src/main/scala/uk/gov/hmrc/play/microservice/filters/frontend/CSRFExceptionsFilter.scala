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

import javax.inject.Inject

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Configuration
import play.api.http.HttpVerbs.POST
import play.api.mvc.{Result, _}
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport

import scala.concurrent.Future

class CSRFExceptionsFilter @Inject() (configuration: Configuration) extends Filter with MicroserviceFilterSupport {

  lazy val whitelist: Set[String] = configuration
    .getStringSeq("csrfexceptions.whitelist")
    .getOrElse(Seq.empty).toSet

  def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(filteredHeaders(rh))
  }

  private[filters] def filteredHeaders(rh: RequestHeader, now: () => DateTime = () => DateTime.now.withZone(DateTimeZone.UTC)) =
    if (rh.method == POST && whitelist.contains(rh.path))
      rh.copy(headers = rh.headers.add("Csrf-Token" -> "nocheck"))
    else rh

}
