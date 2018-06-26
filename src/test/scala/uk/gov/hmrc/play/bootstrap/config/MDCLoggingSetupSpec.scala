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

import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.{Configuration, Mode}

class MDCLoggingSetupSpec extends WordSpec with Matchers with MockitoSugar {

  "New instance of MdcLoggingSetup" should {

    "add the appName to the MDC context" in {
      val mdcPut = mock[(String, String) => Unit]
      new MDCLoggingSetup(Configuration.empty, Mode.Test, "myApp", mdcPut)

      verify(mdcPut).apply("appName", "myApp")
    }

    "add the dateformat when it's set" in {
      val mdcPut = mock[(String, String) => Unit]
      new MDCLoggingSetup(Configuration("logger.json.dateformat" -> "foobar"), Mode.Test, "myApp", mdcPut)

      verify(mdcPut).apply("logger.json.dateformat", "foobar")
    }

    "not add the dateformat when it's not set" in {
      val mdcPut = mock[(String, String) => Unit]
      new MDCLoggingSetup(Configuration.empty, Mode.Test, "myApp", mdcPut)

      verify(mdcPut, never).apply(eqTo("json.logger.dateformat"), any())
    }

  }
}
