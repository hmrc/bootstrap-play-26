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

import javax.inject.Inject

import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.{Configuration, Logger}
import play.mvc.Http.Status

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CacheControlFilter @Inject()(configuration: Configuration) extends Filter with MicroserviceFilterSupport {
  lazy val cachableContentTypes: Seq[String] = {
    val c = configuration.getStringList("caching.allowedContentTypes").toList.flatMap(_.asScala)
    Logger.info(s"Will allow caching of content types matching: ${c.mkString(", ")}")
    c
  }

  final def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    next(rh).map(r =>
      (r.header.status, r.body.contentType) match {
        case (Status.NOT_MODIFIED, _) => r
        case (_, Some(contentType)) if cachableContentTypes.exists(contentType.startsWith) => r
        case _ => r.withHeaders(CommonHeaders.NoCacheHeader)
      }
    )

  }
}