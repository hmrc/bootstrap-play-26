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

package uk.gov.hmrc.play.bootstrap.filters

import javax.inject.Inject

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.{DefaultHttpFilters, HttpFilters}
import play.api.inject.Module
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration, Environment}

object CacheControlFilterSpec {

  class Filters @Inject() (cacheControl: CacheControlFilter) extends DefaultHttpFilters(cacheControl)

  class TestModule extends Module {

    import play.api.inject._

    override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
      Seq(bind[CacheControlConfig].toInstance(CacheControlConfig.fromConfig(configuration)))
  }
}

class CacheControlFilterSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  import CacheControlFilterSpec._

  val router: Router = {

    import play.api.routing.sird._

    Router.from {

      case GET(p"/not-modified") =>
        Action(Results.NotModified)
      case GET(p"/exists") =>
        Action(Results.Ok.withHeaders(CACHE_CONTROL -> "foo"))
      case GET(p"/cacheable") =>
        Action(Results.Ok.withHeaders(CONTENT_TYPE -> "image/foo"))

      case GET(p"/not-modified-and-cacheable") =>
        Action(Results.NotModified.withHeaders(CONTENT_TYPE -> "image/foo"))
      case GET(p"/not-modified-and-exists") =>
        Action(Results.NotModified.withHeaders(CACHE_CONTROL -> "foo"))
      case GET(p"/exists-and-cacheable") =>
        Action(Results.Ok.withHeaders(CONTENT_TYPE -> "image/foo", CACHE_CONTROL -> "foo"))

      case GET(p"/all") =>
        Action(Results.NotModified.withHeaders(CACHE_CONTROL -> "foo", CONTENT_TYPE -> "image/foo"))
    }
  }

  override lazy val app: Application = {

    import play.api.inject._

    new GuiceApplicationBuilder()
      .router(router)
      .overrides(
        new TestModule,
        bind[HttpFilters].to[Filters]
      )
      .build()
  }

  ".apply" must {

    "add the `CACHE_CONTROL` header with relevant values to a result" in {
      val Some(result) = route(app, FakeRequest(GET, "/test"))
      headers(result) must contain(CACHE_CONTROL -> CacheControlFilter.headerValue)
    }

    "not modify the `CACHE_CONTROL` header when it's already set" in {
      val Some(result) = route(app, FakeRequest(GET, "/exists"))
      headers(result) must contain(CACHE_CONTROL -> "foo")
    }

    "not modify the `CACHE_CONTROL` header when the status is `NOT_MODIFIED`" in {
      val Some(result) = route(app, FakeRequest(GET, "/not-modified"))
      headers(result) mustNot contain(CACHE_CONTROL)
    }

    "not modify the `CACHE_CONTROL` header when the content type is cacheable" in {
      val Some(result) = route(app, FakeRequest(GET, "/cacheable"))
      headers(result) mustNot contain(CACHE_CONTROL)
    }

    "not modify the `CACHE_CONTROL` header when the status is `NOT_MODIFIED` and it's already set" in {
      val Some(result) = route(app, FakeRequest(GET, "/not-modified-and-exists"))
      headers(result) must contain(CACHE_CONTROL -> "foo")
    }

    "not modify the `CACHE_CONTROL` header when the status is `NOT_MODIFIED` and the content type is cacheable" in {
      val Some(result) = route(app, FakeRequest(GET, "/not-modified-and-cacheable"))
      headers(result) mustNot contain(CACHE_CONTROL)
    }

    "not modify the `CACHE_CONTROL` header when it's already set and the content type is cacheable" in {
      val Some(result) = route(app, FakeRequest(GET, "/exists-and-cacheable"))
      headers(result) mustNot contain(CACHE_CONTROL)
    }

    "not modify the `CACHE_CONTROL` header when `all of the above`" in {
      val Some(result) = route(app, FakeRequest(GET, "/all"))
      headers(result) must contain(CACHE_CONTROL -> "foo")
    }
  }
}
