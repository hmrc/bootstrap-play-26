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

package uk.gov.hmrc.play.bootstrap.filters.frontend

import javax.inject.Inject
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.http.{DefaultHttpFilters, HeaderNames, HttpFilters}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.CookieSigner
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.{CookieHeaderEncoding, _}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.{DefaultSessionCookieCryptoFilter, SessionCookieCrypto, SessionCookieCryptoFilter}
import play.api.mvc.Session.COOKIE_NAME

object DefaultSessionCookieCryptoFilterSpec {

  class Filters @Inject()(cryptoFilter: SessionCookieCryptoFilter) extends DefaultHttpFilters(cryptoFilter)

//  trait MockCrypto extends Encrypter with Decrypter {
//
//    override def encrypt(plain: PlainContent): Crypted = (plain: @unchecked) match {
//      case PlainText(v) => Crypted(v + "encrypted")
//    }
//
//    override def decrypt(reversiblyEncrypted: Crypted): PlainText =
////      PlainText(reversiblyEncrypted.value + "decrypted")
//      PlainText(reversiblyEncrypted.value)
//
//    // UNUSED
//    override def decryptAsBytes(reversiblyEncrypted: Crypted) = ???
//  }
}

class DefaultSessionCookieCryptoFilterSpec extends WordSpec with MustMatchers with OptionValues with MockitoSugar {

  import DefaultSessionCookieCryptoFilterSpec._

  private val expectedKeyInSession = "foo"

  trait MockedCrypto extends Encrypter with Decrypter

  val mockedCrypto = mock[MockedCrypto]

  private val builder =
    new GuiceApplicationBuilder()
      .routes {
        case ("GET", "/test") =>
          Action { implicit request =>
            Results.Ok.addingToSession("baz" -> "quux")
          }
        case ("GET", "/other-cookie") =>
          Action { implicit request =>
            Results.Ok(request.cookies("womble").value).withCookies(Cookie("fork", "knife"))
          }
      }
      .bindings(
        bind[SessionCookieCrypto].toInstance(SessionCookieCrypto(mockedCrypto))
      )
      .overrides(
        bind[HttpFilters].to[Filters],
        bind[SessionCookieCryptoFilter].to[DefaultSessionCookieCryptoFilter]
      )
      .disable(classOf[CookiesModule])
      .bindings(new LegacyCookiesModule)

  ".apply" must {

    "decrypt the COOKIE header before it gets to the controller" in {

      val app                  = builder.build()
      val sessionBaker         = app.injector.instanceOf(classOf[SessionCookieBaker])
      val cookieHeaderEncoding = app.injector.instanceOf(classOf[CookieHeaderEncoding])

      val sessionCookie = sessionBaker.encodeAsCookie(Session(Map(expectedKeyInSession -> "bar")))
      val request =
        FakeRequest(GET, "/test").withHeaders(
          COOKIE -> cookieHeaderEncoding.encodeCookieHeader(Seq(sessionCookie))
        )

      when(mockedCrypto.decrypt(Crypted(sessionCookie.value))).thenReturn(PlainText(sessionCookie.value))

      Helpers.running(app) {
        val Some(result) = route(
          app,
          request
        )

        status(result) mustBe 200

        verify(mockedCrypto).decrypt(Crypted(sessionCookie.value))
        verifyNoMoreInteractions(mockedCrypto)
      }
    }

    "encrypt the COOKIE header after it's returned from the controller" in {

      val app = builder.build()

      Helpers.running(app) {
        val Some(result) = route(app, FakeRequest(GET, "/test").withSession("foo" -> "bar"))
        header(HeaderNames.SET_COOKIE, result).value must include("encrypted")
        header(HeaderNames.SET_COOKIE, result).value must include("quux")
      }
    }

    "not modify other cookies" in {

      val app = builder.build()

      Helpers.running(app) {
        val Some(result) = route(
          app,
          FakeRequest(GET, "/other-cookie")
            .withCookies(Cookie("womble", "spoon"))
            .withSession("foo" -> "bar"))
        contentAsString(result) mustEqual "spoon"
        cookies(result).get("fork").value.value mustEqual "knife"
      }
    }

//    "remove the COOKIE header when it fails to decrypt" in {
//
//      import play.api.inject._
//
//      val decrypter = mock[Decrypter]
//
//      when(decrypter.decrypt(any())).thenAnswer(new Answer[PlainText] {
//        override def answer(invocation: InvocationOnMock) =
//          throw new Exception()
//      })
//
//      val mockAppCrypto = SessionCookieCrypto(new MockCrypto {
//
//        override def decrypt(reversiblyEncrypted: Crypted) = decrypter.decrypt(reversiblyEncrypted)
//      })
//
//      val app = builder
//        .overrides(
//          bind[SessionCookieCrypto].toInstance(mockAppCrypto)
//        )
//        .build()
//
//      Helpers.running(app) {
//        val Some(result) = route(app, FakeRequest(GET, "/test").withSession("foo" -> "bar"))
//        contentAsString(result) mustEqual ""
//      }
//    }
  }

  private implicit class CookieOps(cookie: Cookie) {
    lazy val asCookieHeaders: Headers = Headers(
      HeaderNames.COOKIE -> Cookies.encodeCookieHeader(Seq(cookie))
    )
  }
}
