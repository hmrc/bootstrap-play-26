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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.{FakeRequest, WithApplication}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SecurityHeaderFilterSpec extends WordSpecLike with Matchers with MockitoSugar with ScalaFutures {


  val appConfigDecodingEnabled: Map[String, _] = Map("security.headers.filter.decoding.enabled" -> true, "play.filters.headers.contentSecurityPolicy" -> "ZGVmYXVsdC1zcmMgJ3NlbGYn")
  val appConfigDecodingDisabled: Map[String, _] = Map("security.headers.filter.decoding.enabled" -> false)

  val appDecodingDisabled: Application = new GuiceApplicationBuilder().configure(appConfigDecodingDisabled).build()
  val appDecodingEnabled: Application = new GuiceApplicationBuilder().configure(appConfigDecodingEnabled).build()

  val auditConnector: AuditConnector = mock[AuditConnector]

  trait Setup {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val action: EssentialAction = Action(Ok("success"))
  }

  "SecurityHeaderFilter" should {

    "add security header to an http response with filter enabled and  settings decoding disabled" in new WithApplication(appDecodingDisabled) with Setup {

      val incomingRequest = FakeRequest()
      val futureResult = new SecurityHeadersFilterFactory().newInstance(action)(incomingRequest).run()

      val result = Await.result(futureResult, Duration.Inf)

      result.header.headers contains "Content-Security-Policy" shouldBe true
      result.header.headers contains "X-Content-Type-Options" shouldBe true
      result.header.headers contains "X-Frame-Options" shouldBe true
      result.header.headers contains "X-Permitted-Cross-Domain-Policies" shouldBe true
      result.header.headers contains "X-XSS-Protection" shouldBe true

      result.header.headers("Content-Security-Policy") shouldBe SecurityHeadersFilterFactory.DEFAULT_CONTENT_SECURITY_POLICY
      result.header.headers("X-Content-Type-Options") shouldBe SecurityHeadersFilterFactory.DEFAULT_CONTENT_TYPE_OPTIONS
      result.header.headers("X-Frame-Options") shouldBe SecurityHeadersFilterFactory.DEFAULT_FRAME_OPTIONS
      result.header.headers("X-Permitted-Cross-Domain-Policies") shouldBe SecurityHeadersFilterFactory.DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES
      result.header.headers("X-XSS-Protection") shouldBe SecurityHeadersFilterFactory.DEFAULT_XSS_PROTECTION

    }

    "add security header to an http response with filter enabled and  settings decoding enabled" in new WithApplication(appDecodingEnabled) with Setup {

      val incomingRequest = FakeRequest()
      val futureResult = new SecurityHeadersFilterFactory().newInstance(action)(incomingRequest).run()

      val result = Await.result(futureResult, Duration.Inf)

      result.header.headers contains "Content-Security-Policy" shouldBe true
      result.header.headers contains "X-Content-Type-Options" shouldBe true
      result.header.headers contains "X-Frame-Options" shouldBe true
      result.header.headers contains "X-Permitted-Cross-Domain-Policies" shouldBe true
      result.header.headers contains "X-XSS-Protection" shouldBe true

      result.header.headers("Content-Security-Policy") shouldBe "default-src 'self'"
      result.header.headers("X-Content-Type-Options") shouldBe SecurityHeadersFilterFactory.DEFAULT_CONTENT_TYPE_OPTIONS
      result.header.headers("X-Frame-Options") shouldBe SecurityHeadersFilterFactory.DEFAULT_FRAME_OPTIONS
      result.header.headers("X-Permitted-Cross-Domain-Policies") shouldBe SecurityHeadersFilterFactory.DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES
      result.header.headers("X-XSS-Protection") shouldBe SecurityHeadersFilterFactory.DEFAULT_XSS_PROTECTION

    }
  }

}
