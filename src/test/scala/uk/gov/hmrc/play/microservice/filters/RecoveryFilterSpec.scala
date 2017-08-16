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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Play
import play.api.mvc.{Action, EssentialAction}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HttpException, NotFoundException}

import scala.concurrent.Future

class RecoveryFilterSpec extends WordSpecLike with Matchers with ScalaFutures with OneAppPerSuite {

  "The RecoveryFilter" should {


    "recover failed actions with 404 status codes" in {

      implicit val mat: Materializer = Play.current.materializer

      val action: EssentialAction = EssentialAction {
        request =>
          Action.async(Future.failed(new NotFoundException("Not found exception")))(request)
      }
      val fResult = new RecoveryFilter().apply(action)(FakeRequest()).run.futureValue
      fResult.header.status shouldBe 404
    }

    "do nothing for actions failed with other status codes" in {

      implicit val mat: Materializer = Play.current.materializer

      val action: EssentialAction = EssentialAction {
        request =>
          Action.async(Future.failed(new HttpException("Internal server error", 500)))(request)
      }
      val fResult = new RecoveryFilter().apply(action)(FakeRequest()).run
      whenReady(fResult.failed) { ex =>
        ex shouldBe a [HttpException]
      }
    }
  }
}
