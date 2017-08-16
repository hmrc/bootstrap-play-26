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

import java.security.cert.X509Certificate

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import play.api.mvc.{Action, EssentialAction, Headers, RequestHeader}
import play.api.test.{FakeHeaders, WithApplication}
import uk.gov.hmrc.http.{HttpException, NotFoundException}

import scala.concurrent.Future

class RecoveryFilterSpec extends WordSpecLike with Matchers with ScalaFutures {

  "The RecoveryFilter" should {

    "recover failed actions with 404 status codes" in new WithApplication {
      val action: EssentialAction = EssentialAction {
        request =>
          Action.async(Future.failed(new NotFoundException("Not found exception")))(request)
      }
      val fResult = new RecoveryFilter().apply(action)(new DummyRequestHeader).run.futureValue
      fResult.header.status shouldBe 404
    }

    "do nothing for actions failed with other status codes" in new WithApplication {
      val action: EssentialAction = EssentialAction {
        request =>
          Action.async(Future.failed(new HttpException("Internal server error", 500)))(request)
      }
      val fResult = new RecoveryFilter().apply(action)(new DummyRequestHeader).run
      whenReady(fResult.failed) { ex =>
        ex shouldBe a [HttpException]
      }
    }
  }
}


class DummyRequestHeader extends RequestHeader {

  override def remoteAddress: String = ???

  override def headers: Headers = FakeHeaders(Seq.empty)

  override def queryString: Map[String, Seq[String]] = ???

  override def version: String = ???

  override def method: String = "GET"

  override def path: String = "/"

  override def uri: String = "/"

  override def tags: Map[String, String] = ???

  override def id: Long = ???

  override def secure: Boolean = false

  override def clientCertificateChain: Option[Seq[X509Certificate]] = None
}
