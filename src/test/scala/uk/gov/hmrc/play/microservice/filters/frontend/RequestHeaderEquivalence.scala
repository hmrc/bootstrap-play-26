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

import org.scalactic.Equivalence
import play.api.mvc.RequestHeader

object RequestHeaderEquivalence extends Equivalence[RequestHeader] {

  def areEquivalent(h1: RequestHeader, h2: RequestHeader): Boolean = {
    h1.id == h2.id &&
    h1.tags == h2.tags &&
    h1.uri == h2.uri &&
    h1.path == h2.path &&
    h1.method == h2.method &&
    h1.version == h2.version &&
    h1.queryString == h2.queryString &&
    h1.headers.toMap == h2.headers.toMap &&
    h1.remoteAddress == h2.remoteAddress
  }
}
