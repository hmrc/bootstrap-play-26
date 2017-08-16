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

package uk.gov.hmrc.play.microservice.http

import java.security.cert.X509Certificate

import akka.stream.Materializer
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerTest
import play.api.http.HttpEntity
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.Helpers._
import play.twirl.api.Html

class FrontendErrorHandlerSpec extends WordSpecLike with Matchers with OneAppPerTest {

  implicit lazy val materializer: Materializer = app.materializer
  import play.api.i18n.Messages.Implicits._

  object TestFrontendErrorHandler extends FrontendErrorHandler {
    override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html = Html("error")
  }

  import TestFrontendErrorHandler._

  "resolving an error" should {
    "return a generic InternalServerError result" in {
      val exception = new Exception("Runtime exception")
      val result = resolveError(FakeRequestHeader, exception)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
      result.header.headers should contain (CACHE_CONTROL -> "no-cache")
    }

    "return a generic InternalServerError result if the exception cause is null" in {
      val exception = new Exception("Runtime exception", null)
      val result = resolveError(FakeRequestHeader, exception)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
      result.header.headers should contain (CACHE_CONTROL -> "no-cache")
    }

    "return an InternalServerError result for an application error" in {

      val responseCode = SEE_OTHER
      val location = "http://some.test.location/page"
      val theResult = Result(
        ResponseHeader(responseCode, Map("Location" -> location)), HttpEntity.NoEntity
      )

      val appException = new ApplicationException("paye", theResult, "application exception")

      val result = resolveError(FakeRequestHeader, appException)

      result shouldBe theResult
    }
  }
}

case object FakeRequestHeader extends RequestHeader {
  override def id: Long = 0L

  override def remoteAddress: String = ""

  override def headers: Headers = FakeHeaders()

  override def queryString: Map[String, Seq[String]] = Map.empty

  override def version: String = ""

  override def method: String = "GET"

  override def path: String = "some-path"

  override def uri: String = ""

  override def tags: Map[String, String] = Map.empty

  override def secure: Boolean = false

  override def clientCertificateChain: Option[Seq[X509Certificate]] = None
}
