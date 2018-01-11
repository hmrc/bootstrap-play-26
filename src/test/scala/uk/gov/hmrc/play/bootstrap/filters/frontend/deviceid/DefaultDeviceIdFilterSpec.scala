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

import javax.inject.Inject

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.Application
import play.api.http.{DefaultHttpFilters, HeaderNames, HttpFilters}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

object DefaultDeviceIdFilterSpec {

  class Filters @Inject() (deviceId: DeviceIdFilter) extends DefaultHttpFilters(deviceId)
}

class DefaultDeviceIdFilterSpec extends WordSpecLike with Matchers with MockitoSugar with OptionValues with ScalaFutures {

  import DefaultDeviceIdFilterSpec._

  val theSecret = "some_secret"
  val thePreviousSecret = "some previous secret with spaces since spaces cause an issue unless encoded!!!"

  val createDeviceId = new DeviceIdCookie {
    override val secret = theSecret
    override val previousSecrets = Seq(thePreviousSecret)
  }

  val appConfigNoPreviousKey: Map[String, Any] = Map("cookie.deviceId.secret" -> theSecret)
  val appConfig: Map[String, Any] = appConfigNoPreviousKey + ("cookie.deviceId.previous.secret" -> Seq(thePreviousSecret))

  val auditConnector: AuditConnector = mock[AuditConnector]

  val builder: GuiceApplicationBuilder = {

    import play.api.inject._

    new GuiceApplicationBuilder()
      .bindings(
        bind[DeviceIdFilter].to[DefaultDeviceIdFilter],
        bind[AuditConnector].toInstance(auditConnector)
      )
      .overrides(
        bind[HttpFilters].to[Filters]
      )
  }

  "DeviceIdFilter" should {

    "create the deviceId when no cookie exists" in {

      val app: Application = builder.configure(appConfig).build()

      running(app) {
        val Some(result) = route(app, FakeRequest(GET, "/test"))
        header(HeaderNames.SET_COOKIE, result) shouldBe defined
      }
    }

    "create the deviceId when no cookie exists and previous keys are empty" in {

      val app: Application = builder.configure(appConfigNoPreviousKey).build()

      running(app) {
        val Some(result) = route(app, FakeRequest(GET, "/test"))
        header(HeaderNames.SET_COOKIE, result) shouldBe defined
      }
    }


    "do nothing when a valid cookie exists" in {

      val app: Application = builder.configure(appConfig).build()

      running(app) {
        val existingCookie = createDeviceId.buildNewDeviceIdCookie()
        val Some(result) = route(app, FakeRequest(GET, "/test").withCookies(existingCookie))
        cookies(result) should contain(existingCookie)
      }
    }

    "successfully decode a deviceId generated from a previous secret" in {

      val app: Application = builder.configure(appConfig).build()

      running(app) {

        val uuid = createDeviceId.generateUUID

        val existingCookie = {
          val timestamp = createDeviceId.getTimeStamp
          val deviceIdMadeFromPrevKey = DeviceId(uuid, timestamp, DeviceId.generateHash(uuid, timestamp, thePreviousSecret))
          createDeviceId.makeCookie(deviceIdMadeFromPrevKey)
        }

        val Some(result) = route(app, FakeRequest(GET, "/test").withCookies(existingCookie))

        cookies(result).get(DeviceId.MdtpDeviceId).value.value should include(uuid)
      }
    }
  }
}
