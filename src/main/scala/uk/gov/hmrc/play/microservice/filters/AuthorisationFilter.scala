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

import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Filter, RequestHeader, Result, Results}
import play.api.routing.Router.Tags
import uk.gov.hmrc.auth.core.authorise.RawJsonPredicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.auth.filter.{AuthConfig, FilterConfig, PathMatcher}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthorisationFilter extends Filter {

  def config: FilterConfig

  def connector: AuthConnector

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    def applyPathMatcher(pathMatchers: Seq[PathMatcher]): Option[Map[String, String]] =
      if (pathMatchers.isEmpty) None
      else pathMatchers.head.matchPath(rh.path).orElse(
        applyPathMatcher(pathMatchers.tail)
      )

    def prepareJson(config: AuthConfig, resolvedPathVariables: Map[String, String]): JsArray = {
      val unresolvedJson = config.predicatesAsJson
      val resolvedJson = resolvedPathVariables.foldLeft(unresolvedJson) {
        case (json, (key, value)) => json.replaceAll("\\$" + key, value)
      }
      Json.parse(resolvedJson).as[JsArray]
    }

    def applyConfig(configs: Seq[AuthConfig]): Future[Result] =
      if (configs.isEmpty) Future.successful(Results.Unauthorized)
      else applyPathMatcher(configs.head.pathMatchers).fold(
        applyConfig(configs.tail)
      ) { resolvedPathVariables =>
        val parsedJson = prepareJson(configs.head, resolvedPathVariables)
        connector
          .authorise(RawJsonPredicate(parsedJson), EmptyRetrieval).flatMap(_ => next(rh))
          .recover { case _: AuthorisationException => Results.Unauthorized }
      }

    val authConfig = rh.tags
      .get(Tags.RouteController)
      .fold(Seq[AuthConfig]())(con => config.getConfigForController(con))

    authConfig match {
      case Seq() => next(rh)
      case configs =>
        val result = applyConfig(configs)
        result
    }
  }

}
