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

import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec

class AppNameSpec extends UnitSpec {

  "AppName" should {
    "return the name of the application" in new TestSetup {
      override def configuration: Configuration = Configuration("appName" -> "myApp")

      appName shouldBe "myApp"
    }

    "return fallback name if application name not available" in new TestSetup {
      override def configuration: Configuration = Configuration()

      appName shouldBe "APP NAME NOT SET"
    }
  }

  trait TestSetup extends AppName
}
