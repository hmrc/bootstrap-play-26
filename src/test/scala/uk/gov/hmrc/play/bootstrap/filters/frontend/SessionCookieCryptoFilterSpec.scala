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

package uk.gov.hmrc.play.bootstrap.filters.frontend

import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Cookie, RequestHeader, Result, Session, _}
import play.api.test.{FakeApplication, FakeRequest, Helpers}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, PlainText}

import scala.concurrent.Future

class SessionCookieCryptoFilterSpec extends WordSpecLike with Matchers with MockitoSugar with ScalaFutures with OneAppPerSuite {

  val action = {
    val mockAction = mock[(RequestHeader) => Future[Result]]
    val outgoingResponse = Future.successful(Results.Ok.withHeaders(HeaderNames.SET_COOKIE -> Cookies.encodeSetCookieHeader(Seq(Cookie(Session.COOKIE_NAME, "our-new-cookie")))))
    when(mockAction.apply(any())).thenReturn(outgoingResponse)
    mockAction
  }

  def requestPassedToAction: RequestHeader = {
    val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
    verify(action).apply(updatedRequest.capture())
    updatedRequest.getValue
  }

  "SessionCookieCryptoFilter" should {

    def createEncryptedCookie(cookieVal: String) = Cookie(Session.COOKIE_NAME, ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(cookieVal)).value)

    // TODO this filter uses global state and will be removed
    "decrypt the session cookie on the way in and encrypt it again on the way back" ignore {


        val encryptedIncomingCookie = createEncryptedCookie("our-cookie")
        val unencryptedIncomingCookie = Cookie(Session.COOKIE_NAME, "our-cookie")

        val incomingRequest = FakeRequest().withCookies(encryptedIncomingCookie)
        val response = SessionCookieCryptoFilter(action)(incomingRequest).futureValue

        requestPassedToAction.cookies(Session.COOKIE_NAME) shouldBe unencryptedIncomingCookie

        val encryptedOutgoingCookieValue = Cookies.decodeSetCookieHeader(response.header.headers(HeaderNames.SET_COOKIE))(0).value
        ApplicationCrypto.SessionCookieCrypto.decrypt(Crypted(encryptedOutgoingCookieValue)).value shouldBe "our-new-cookie"
    }
  }
}
