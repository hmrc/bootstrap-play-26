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

import akka.stream.Materializer
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait CookieCryptoFilter extends Filter {

  implicit def mat: Materializer

  protected lazy val cookieName: String = Session.COOKIE_NAME
  protected val encrypter: (String) => String
  protected val decrypter: (String) => String

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) =
    encryptCookie(next(decryptCookie(rh)))

  private def decryptCookie(rh: RequestHeader) = {
    val updatedCookies = (
      for {
        cookie <- rh.headers.getAll(HeaderNames.COOKIE)
        decoded <- Cookies.decodeCookieHeader(cookie)
        decrypted = decrypt(decoded)
      } yield decrypted).flatten

    if (updatedCookies.isEmpty)
      rh.copy(headers = rh.headers.remove(HeaderNames.COOKIE))
    else
      rh.copy(headers = rh.headers.replace(HeaderNames.COOKIE -> Cookies.encodeCookieHeader(updatedCookies)))
  }

  def tryDecrypting(value: String): Option[String] = Try(decrypter(value)) match {
    case Success(v) => Some(v)
    case Failure(ex) =>
      Logger.warn(s"Could not decrypt cookie $cookieName got exception:${ex.getMessage}")
      None
  }

  def decrypt(cookie: Cookie): Option[Cookie] = {
    if (shouldBeEncrypted(cookie))
      tryDecrypting(cookie.value).map { decryptedValue =>
        cookie.copy(value = decryptedValue)
      }
    else Some(cookie)
  }


  private def encryptCookie(f: Future[Result]): Future[Result] = f.map {
    result =>
      val updatedHeader: Option[String] = result.header.headers.get(HeaderNames.SET_COOKIE).map {
        cookieHeader =>
          Cookies.encodeSetCookieHeader(Cookies.decodeSetCookieHeader(cookieHeader).map { cookie: Cookie =>
            if (shouldBeEncrypted(cookie))
              cookie.copy(value = encrypter(cookie.value))
            else
              cookie
          })
      }

      updatedHeader.map(header => result.withHeaders(HeaderNames.SET_COOKIE -> header)).getOrElse(result)
  }

  private def shouldBeEncrypted(cookie: Cookie) = cookie.name == cookieName && !cookie.value.isEmpty
}
