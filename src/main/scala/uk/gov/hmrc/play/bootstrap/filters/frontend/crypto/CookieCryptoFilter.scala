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

package uk.gov.hmrc.play.bootstrap.filters.frontend.crypto

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Session.COOKIE_NAME
import play.api.mvc._
import uk.gov.hmrc.crypto._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

trait CryptoImplicits {

  protected implicit def strToPlain(s: String): PlainContent =
    PlainText(s)

  protected implicit def strToCrypt(s: String): Crypted =
    Crypted(s)

  protected implicit def plainToString(p: PlainText): String =
    p.value

  protected implicit def cryptToString(c: Crypted): String =
    c.value
}

trait CookieCryptoFilter extends Filter with CryptoImplicits {

  protected implicit def ec: ExecutionContext

  protected def encrypter: Encrypter
  protected def decrypter: Decrypter

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] =
    encryptCookie(next(decryptCookie(rh)))

  private def decryptCookie(rh: RequestHeader) = {
    val updatedCookies = (for {
      cookie  <- rh.headers.getAll(HeaderNames.COOKIE)
      decoded <- Cookies.decodeCookieHeader(cookie)
      decrypted = decrypt(decoded)
    } yield decrypted).flatten

    if (updatedCookies.isEmpty) {
      rh.copy(headers = rh.headers.remove(HeaderNames.COOKIE))
    } else {
      rh.copy(headers = rh.headers.replace(HeaderNames.COOKIE -> Cookies.encodeCookieHeader(updatedCookies)))
    }
  }

  private def tryDecrypting(value: String): Option[String] = Try(decrypter.decrypt(value)) match {
    case Success(v) => Some(v)
    case Failure(ex) =>
      Logger.warn(s"Could not decrypt cookie $COOKIE_NAME got exception:${ex.getMessage}")
      None
  }

  private def decrypt(cookie: Cookie): Option[Cookie] =
    if (shouldBeEncrypted(cookie))
      tryDecrypting(cookie.value).map { decryptedValue =>
        cookie.copy(value = decryptedValue)
      } else Some(cookie)

  private def encryptCookie(f: Future[Result]): Future[Result] = f.map { result =>
    val updatedHeader: Option[String] = result.header.headers.get(HeaderNames.SET_COOKIE).map { cookieHeader =>
      Cookies.encodeSetCookieHeader(Cookies.decodeSetCookieHeader(cookieHeader).map { cookie: Cookie =>
        if (shouldBeEncrypted(cookie))
          cookie.copy(value = encrypter.encrypt(cookie.value))
        else
          cookie
      })
    }

    updatedHeader.map(header => result.withHeaders(HeaderNames.SET_COOKIE -> header)).getOrElse(result)
  }

  private def shouldBeEncrypted(cookie: Cookie) = cookie.name == COOKIE_NAME && !cookie.value.isEmpty
}

class DefaultCookieCryptoFilter @Inject()(
  sessionCookieCrypto: SessionCookieCrypto
)(
  implicit
  override val mat: Materializer,
  override val ec: ExecutionContext)
    extends CookieCryptoFilter {

  override protected lazy val encrypter: Encrypter = sessionCookieCrypto.crypto
  override protected lazy val decrypter: Decrypter = sessionCookieCrypto.crypto
}
