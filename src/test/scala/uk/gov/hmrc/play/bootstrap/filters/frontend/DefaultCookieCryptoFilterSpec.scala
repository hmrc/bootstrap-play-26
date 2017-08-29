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

import javax.inject.Inject

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.http.{DefaultHttpFilters, HeaderNames, HttpFilters}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, Cookie, Results}
import play.api.routing.Router
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.crypto._

object DefaultCookieCryptoFilterSpec {

  class Filters @Inject() (cryptoFilter: CookieCryptoFilter) extends DefaultHttpFilters(cryptoFilter)
}

class DefaultCookieCryptoFilterSpec extends WordSpec with MustMatchers with OptionValues with MockitoSugar {

  import DefaultCookieCryptoFilterSpec._

  val router: Router = {

    import play.api.routing.sird._

    Router.from {
      case GET(p"/test") =>
        Action { implicit request =>
          Results.Ok(
            request.headers.get(HeaderNames.COOKIE).getOrElse("")
          ).addingToSession("baz" -> "quux")
        }
      case GET(p"/other-cookie") =>
        Action { implicit request =>
          Results.Ok(request.cookies("womble").value).withCookies(Cookie("fork", "knife"))
        }
    }
  }

  val mockCrypto = new Encrypter with Decrypter {

    override def encrypt(plain: PlainContent): Crypted = (plain: @unchecked) match {
      case PlainText(v) => Crypted(v + "encrypted")
    }

    override def decrypt(reversiblyEncrypted: Crypted): PlainText =
      PlainText(reversiblyEncrypted.value + "decrypted")

    // UNUSED
    override def decryptAsBytes(reversiblyEncrypted: Crypted) = ???
  }

  ".apply" must {

    val builder = {

      import play.api.inject._

      new GuiceApplicationBuilder()
        .router(router)
        .bindings(
          bind[Encrypter].toInstance(mockCrypto),
          bind[Decrypter].toInstance(mockCrypto)
        )
        .overrides(
          bind[HttpFilters].to[Filters],
          bind[CookieCryptoFilter].to[DefaultCookieCryptoFilter]
        )
    }

    "decrypt the COOKIE header before it gets to the controller" in {

      val app = builder.build()

      Helpers.running(app) {
        val Some(result) = route(app, FakeRequest(GET, "/test").withSession("foo" -> "bar"))
        contentAsString(result) must endWith("decrypted")
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
        val Some(result) = route(app, FakeRequest(GET, "/other-cookie")
          .withCookies(Cookie("womble", "spoon"))
          .withSession("foo" -> "bar"))
        contentAsString(result) mustEqual "spoon"
        cookies(result).get("fork").value.value mustEqual "knife"
      }
    }

    "remove the COOKIE header when it fails to decrypt" in {

      import play.api.inject._

      val decrypter = mock[Decrypter]

      when(decrypter.decrypt(any())).thenAnswer(new Answer[PlainText] {
        override def answer(invocation: InvocationOnMock) =
          throw new Exception()
      })

      val app = builder
        .overrides(
          bind[Decrypter].toInstance(decrypter)
        )
        .build()

      Helpers.running(app) {
        val Some(result) = route(app, FakeRequest(GET, "/test").withSession("foo" -> "bar"))
        contentAsString(result) mustEqual ""
      }
    }
  }
}
