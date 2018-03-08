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

package uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid

import play.api.http.HeaderNames
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}

import scala.concurrent.{ExecutionContext, Future}

trait DeviceIdFilter extends Filter with DeviceIdCookie {

  protected implicit def ec: ExecutionContext

  protected def auditConnector: AuditConnector
  protected def appName: String

  case class CookeResult(cookies: Seq[Cookie], newDeviceIdCookie: Cookie)

  private def isDeviceIdCookie(cookie: Cookie): Boolean = cookie.name == DeviceId.MdtpDeviceId && !cookie.value.isEmpty

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
    val requestCookies = rh.headers.getAll(HeaderNames.COOKIE).flatMap(Cookies.decodeCookieHeader)

    def allCookiesApartFromDeviceId = requestCookies.filterNot(_.name == DeviceId.MdtpDeviceId)

    val find: Option[Cookie] = requestCookies.find(isDeviceIdCookie)
    val cookieResult = find
      .map { deviceCookeValueId =>
        DeviceId.from(deviceCookeValueId.value, secret, previousSecrets) match {

          case Some(deviceId) =>
            // Valid new format cookie.
            // Ensure the cookie is secure by setting it again with the secure flag
            val secureDeviceIdCookie = buildNewDeviceIdCookie().copy(value = deviceId.value)
            CookeResult(allCookiesApartFromDeviceId :+ secureDeviceIdCookie, secureDeviceIdCookie)

          case None =>
            // Invalid deviceId cookie. Replace invalid cookie from request with new deviceId cookie and return in response.
            val deviceIdCookie = buildNewDeviceIdCookie()
            sendDataEvent(rh, deviceCookeValueId.value, deviceIdCookie.value)
            CookeResult(allCookiesApartFromDeviceId ++ Seq(deviceIdCookie), deviceIdCookie)
        }
      }
      .getOrElse {
        // No deviceId cookie found or empty cookie value. Create new deviceId cookie, add to request and response.
        val newDeviceIdCookie = buildNewDeviceIdCookie()
        CookeResult(allCookiesApartFromDeviceId ++ Seq(newDeviceIdCookie), newDeviceIdCookie)
      }

    val newCookie           = HeaderNames.COOKIE -> Cookies.encodeSetCookieHeader(cookieResult.cookies)
    val updatedInputHeaders = rh.copy(headers = rh.headers.replace(newCookie))

    next(updatedInputHeaders).map(theHttpResponse => {
      theHttpResponse.withCookies(cookieResult.newDeviceIdCookie)
    })

  }

  private def sendDataEvent(rh: RequestHeader, badDeviceId: String, goodDeviceId: String): Unit = {
    val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)
    auditConnector.sendEvent(
      DataEvent(
        appName,
        EventTypes.Failed,
        tags   = hc.toAuditTags("deviceIdFilter", rh.path),
        detail = getTamperDetails(badDeviceId, goodDeviceId)))
  }

  private def getTamperDetails(tamperDeviceId: String, newDeviceId: String) =
    Map("tamperedDeviceId" -> tamperDeviceId, "deviceID" -> newDeviceId)

}
