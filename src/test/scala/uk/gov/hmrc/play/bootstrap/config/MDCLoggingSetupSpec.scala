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

package uk.gov.hmrc.play.bootstrap.config

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}

class MDCLoggingSetupSpec extends WordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach {

  val mdc: MDCInstance = mock[MDCInstance]

  val builder: GuiceApplicationBuilder = {

    import play.api.inject._

    new GuiceApplicationBuilder()
      .bindings(
        bind[MDCInstance].toInstance(mdc),
        bind[MDCLoggingSetup].toSelf.eagerly
      )
      .configure(
        "appName" -> "Test"
      )
  }

  override def beforeEach(): Unit = {
    reset(mdc)
    super.beforeEach()
  }

  "new" must {

    "add the appName to the MDC context" in {
      builder.build()
      verify(mdc).put("appName", "Test")
    }

    "not add the dateformat when it's not set" in {
      builder.build()
      verify(mdc, never).put(eqTo("json.logger.dateformat"), any())
    }

    "add the dateformat when it's set" in {
      builder.configure("logger.json.dateformat" -> "foobar").build()
      verify(mdc).put("logger.json.dateformat", "foobar")
    }
  }
}
