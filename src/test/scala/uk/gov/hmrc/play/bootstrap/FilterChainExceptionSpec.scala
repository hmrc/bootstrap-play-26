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

package uk.gov.hmrc.play.bootstrap

import javax.inject.Inject

import org.scalatest.{Matchers, TestData, WordSpecLike}
import org.scalatestplus.play.OneServerPerTest
import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.filters.headers.SecurityHeadersFilter
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class FiltersForTestWithSecurityFilterFirst @Inject()(securityHeaderFilter: SecurityHeadersFilter) extends HttpFilters {
  def filters = Seq(securityHeaderFilter)
}

class FilterChainExceptionSecurityFirstSpec extends WordSpecLike with Matchers with WsTestClient with OneServerPerTest {

  val routerForTest: Router = Router.from {
    case GET(p"/ok") =>
      Action { request =>
        Results.Ok("OK")
      }
    case GET(p"/error-async-404") =>
      Action { request =>
        throw new NotFoundException("Expect 404")
      }
  }

  override def newAppForTest(testData: TestData): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[HttpFilters].to[FiltersForTestWithSecurityFilterFirst]
      )
      .router(routerForTest)
      .build()

  "Action throws no exception and returns 200 OK" in {
    val response = Await.result(wsUrl("/ok")(port).get(), Duration.Inf)
    response.status shouldBe (200)
    response.body   shouldBe ("OK")
  }

  "No endpoint in router and returns 404" in {
    val response = Await.result(wsUrl("/no-end-point")(port).get(), Duration.Inf)
    response.status shouldBe (404)
  }
}
