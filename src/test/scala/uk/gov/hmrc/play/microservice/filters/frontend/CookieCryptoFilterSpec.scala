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
import akka.stream.{ActorMaterializer, Materializer}
import ch.qos.logback.classic.Level
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.LogCapturing

import scala.concurrent.Future

class CookieCryptoFilterSpec extends WordSpecLike with ScalaFutures with Matchers with LogCapturing with LoneElement with MockitoSugar with TypeCheckedTripleEquals {

  private trait Setup extends Results {
    implicit val headerEquiv = RequestHeaderEquivalence

    val cookieName = "someCookieName"
    val encryptedCookie = Cookie(cookieName, "encryptedValue")
    val unencryptedCookie = encryptedCookie.copy(value = "decryptedValue")
    val corruptEncryptedCookie = encryptedCookie.copy(value = "invalidEncryptedValue")
    val emptyCookie = encryptedCookie.copy(value = "")

    val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
    val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")

    val resultFromAction: Result = Ok

    lazy val action = {
      val mockAction = mock[(RequestHeader) => Future[Result]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    def filter = new CookieCryptoFilter {

      implicit val system = ActorSystem("test")
      implicit val mat: Materializer = ActorMaterializer()

      override lazy val cookieName = Setup.this.cookieName
      protected val encrypter = encrypt _
      protected val decrypter = decrypt _

      private def encrypt(plainValue: String) = plainValue match {
        case "decryptedValue" => "encryptedValue"
        case _ => "notfound"
      }

      private def decrypt(encryptedValue: String) = encryptedValue match {
        case "encryptedValue" => "decryptedValue"
        case _ => throw new Exception("Couldn't decrypt that")
      }
    }

    def requestPassedToAction: RequestHeader = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue
    }
  }

  "During request pre-processing, the filter" should {

    "do nothing with no cookie header in the request" in new Setup {
      val incomingRequest = FakeRequest()
      filter(action)(incomingRequest)
      requestPassedToAction should ===(incomingRequest)
    }

    "decrypt the cookie" in new Setup {
      val incomingRequest = FakeRequest().withCookies(encryptedCookie)
      filter(action)(incomingRequest)
      requestPassedToAction should ===(FakeRequest().withCookies(unencryptedCookie))
    }

    "leave empty cookies unchanged" in new Setup {
      val incomingRequest = FakeRequest().withCookies(emptyCookie)
      filter(action)(incomingRequest)
      requestPassedToAction should ===(incomingRequest)
    }

    "leave other cookies alone if our cookie is not present" in new Setup {
      val incomingRequest = FakeRequest().withCookies(normalCookie1, normalCookie2)
      filter(action)(incomingRequest)
      requestPassedToAction should ===(incomingRequest)
    }

    "leave other cookies alone when ours is present" in new Setup {
      val incomingRequest = FakeRequest().withCookies(normalCookie1, encryptedCookie, normalCookie2)
      filter(action)(incomingRequest)
      requestPassedToAction should ===(FakeRequest().withCookies(unencryptedCookie, normalCookie1, normalCookie2))
    }

    "remove the cookie header if the decryption fails and there are no other cookies" in new Setup {
      val incomingRequest = FakeRequest().withCookies(corruptEncryptedCookie)
      filter(action)(incomingRequest)
      requestPassedToAction should ===(FakeRequest())
    }

    "remove the cookie (but leave other cookies intact) if with the decryption fails" in new Setup {
      val incomingRequest = FakeRequest().withCookies(normalCookie1, corruptEncryptedCookie, normalCookie2)
      withCaptureOfLoggingFrom(Logger) { logEvents =>
        filter(action)(incomingRequest)
        requestPassedToAction should === (FakeRequest().withCookies(normalCookie1, normalCookie2))
        logEvents.filter(_.getLevel == Level.WARN).loneElement.toString should include("Could not decrypt cookie")
      }
    }
  }

  "During result post-processing, the filter" should {

    "do nothing with the result if there are no cookies" in new Setup {
      filter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "do nothing with the result if there are cookies, but not our cookie" in new Setup {
      override val resultFromAction = Ok.withCookies(normalCookie1, normalCookie2)
      filter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "encrypt the cookie value before returning it" in new Setup {
      override val resultFromAction = Ok.withCookies(unencryptedCookie)
      filter(action)(FakeRequest()).futureValue should be(Ok.withCookies(encryptedCookie))
    }

    "encrypt the cookie value before returning it, leaving other cookies unchanged" in new Setup {
      override val resultFromAction = Ok.withCookies(normalCookie1, unencryptedCookie, normalCookie2)
      filter(action)(FakeRequest()).futureValue should be(Ok.withCookies(normalCookie1, encryptedCookie, normalCookie2))
    }

    "do nothing with the cookie value if it is empty" in new Setup {
      override val resultFromAction = Ok.withCookies(emptyCookie)
      filter(action)(FakeRequest()).futureValue should be(Ok.withCookies(emptyCookie))
    }
  }

}
