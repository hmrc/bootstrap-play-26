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

import akka.stream.Materializer
import play.api.Play
import play.api.Play.current
import play.api.mvc.Filter
import play.mvc.Http.HeaderNames

object CommonHeaders {
  val NoCacheHeader = HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0"
}

trait MicroserviceFilterSupport {
  implicit def mat: Materializer = Play.materializer
}
