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
import org.mockito.Matchers._
import org.mockito.Mockito.{times, _}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}

import scala.concurrent.Future

class DeviceIdFilterSpec extends WordSpecLike with Matchers with OneAppPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfterEach with TypeCheckedTripleEquals with Inspectors with OptionValues {

  lazy val timestamp = System.currentTimeMillis()

  private trait Setup extends Results {
    val normalCookie = Cookie("AnotherCookie1", "normalValue1")

    val resultFromAction: Result = Ok

    lazy val action = {
      val mockAction = mock[(RequestHeader) => Future[Result]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    lazy val filter = new DeviceIdFilter {
      implicit val system = ActorSystem("test")
      implicit val mat: Materializer = ActorMaterializer()

      lazy val mdtpCookie = super.buildNewDeviceIdCookie()

      override def getTimeStamp = timestamp

      override def buildNewDeviceIdCookie() = mdtpCookie

      override val secret = "SOME_SECRET"
      override val previousSecrets = Seq("previous_key_1", "previous_key_2")

      override val appName = "SomeAppName"

      lazy val auditConnector = mock[AuditConnector]
    }

    lazy val newFormatGoodCookieDeviceId = filter.mdtpCookie


    def requestPassedToAction(time:Option[Int]=None): RequestHeader = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action, time.fold(times(1))(count=>Mockito.atMost(count))).apply(updatedRequest.capture())
      updatedRequest.getValue
    }

    def mdtpdiSetCookie(result: Result): Cookie = {
      val cookie = for {
        header <- result.header.headers.get("Set-Cookie")
        setCookies = Cookies.decodeSetCookieHeader(header)
        deviceCookie <- setCookies.find(_.name == DeviceId.MdtpDeviceId)
      }
      yield deviceCookie
      cookie.value
    }

    def generateDeviceIdLegacy(uuid: String = filter.generateUUID): DeviceId = DeviceId(uuid, None, DeviceId.generateHash(uuid, None, filter.secret))

    def expectAuditIdEvent(badCookie: String, validCookie: String) = {
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(filter.auditConnector).sendEvent(captor.capture())(any(), any())
      val event = captor.getValue

      event.auditType shouldBe EventTypes.Failed
      event.auditSource shouldBe "SomeAppName"

      event.detail should contain("tamperedDeviceId" -> badCookie)
      event.detail should contain("deviceID" -> validCookie)
    }


    def invokeFilter(cookies: Seq[Cookie], expectedResultCookie: Cookie, times:Option[Int]=None) = {
      val incomingRequest = if (cookies.isEmpty) FakeRequest() else FakeRequest().withCookies(cookies: _*)
      val result = filter(action)(incomingRequest).futureValue

      val expectedCookie = requestPassedToAction(times).cookies.get(DeviceId.MdtpDeviceId).get
      expectedCookie.value shouldBe expectedResultCookie.value

      result
    }
  }

  "The filter supporting multiple previous hash secrets" should {

    "successfully validate the hash of deviceId's built from more than one previous key" in new Setup {

      for (prevSecret <- filter.previousSecrets) {
        val uuid = filter.generateUUID
        val timestamp = filter.getTimeStamp
        val deviceIdMadeFromPrevKey = DeviceId(uuid, Some(timestamp), DeviceId.generateHash(uuid, Some(timestamp), prevSecret))
        val cookie = filter.makeCookie(deviceIdMadeFromPrevKey)

        val result = invokeFilter(Seq(cookie), cookie, Some(2))

        val responseCookie = mdtpdiSetCookie(result)
        responseCookie.value shouldBe deviceIdMadeFromPrevKey.value
        responseCookie.secure shouldBe true
      }
    }
  }

  "During request pre-processing, the filter" should {

    "create a new deviceId if the deviceId cookie received contains an empty value " in new Setup {
      val result = invokeFilter(Seq(newFormatGoodCookieDeviceId.copy(value="")), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "create new deviceId cookie when no cookies exists" in new Setup {
      val result = invokeFilter(Seq.empty, newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "not change the request or the response when a valid new format mtdpdi cookie exists" in new Setup {
      val result = invokeFilter(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId)

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "make an insecure mtdpdi cookie secure, keeping the same value" in new Setup {
      val result = invokeFilter(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId.copy(secure = false))

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "auto convert legacy DeviceId cookie to new format" in new Setup {

      val (legacyDeviceIdCookie, newFormatDeviceIdCookie) = {
        val testUUID = filter.generateUUID

        val legacyDeviceId = generateDeviceIdLegacy(testUUID)
        val currentDeviceId = filter.generateDeviceId(testUUID)

        val legacyCookieValue = Cookie(DeviceId.MdtpDeviceId, legacyDeviceId.value, Some(DeviceId.TenYears))
        val newFormatCookieValue = Cookie(DeviceId.MdtpDeviceId, currentDeviceId.value, Some(DeviceId.TenYears))

        (legacyCookieValue, newFormatCookieValue)
      }

      val result = invokeFilter(Seq(legacyDeviceIdCookie), newFormatDeviceIdCookie)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatDeviceIdCookie.value
      responseCookie.secure shouldBe true
    }

    "identify legacy deviceId cookie is invalid and create new deviceId cookie" in new Setup {

      val legacyFormatBadCookieDeviceId = {
        val legacyDeviceId = generateDeviceIdLegacy().copy(hash="wrongvalue")
        Cookie(DeviceId.MdtpDeviceId, legacyDeviceId.value, Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(legacyFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true

      expectAuditIdEvent(legacyFormatBadCookieDeviceId.value,newFormatGoodCookieDeviceId.value)
    }

    "identify new format deviceId cookie has invalid hash and create new deviceId cookie" in new Setup {

      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId().copy(hash="wrongvalue")
        Cookie(DeviceId.MdtpDeviceId, deviceId.value, Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)


      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true

      expectAuditIdEvent(newFormatBadCookieDeviceId.value,newFormatGoodCookieDeviceId.value)
    }

    "identify new format deviceId cookie has invalid timestamp and create new deviceId cookie" in new Setup {

      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId().copy(hash="wrongvalue")
        Cookie(DeviceId.MdtpDeviceId, deviceId.value, Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)


      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true

      expectAuditIdEvent(newFormatBadCookieDeviceId.value,newFormatGoodCookieDeviceId.value)
    }

    "identify new format deviceId cookie has invalid prefix and create new deviceId cookie" in new Setup {

      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId()
        Cookie(DeviceId.MdtpDeviceId, deviceId.value.replace(DeviceId.MdtpDeviceId,"BAD_PREFIX"), Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

  }

}
