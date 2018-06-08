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

package uk.gov.hmrc.play.config

import org.scalatest._
import play.api.Configuration

class AppNameSpec extends WordSpecLike with Matchers {

  "AppName trait" should {
    "return app name if defined" in new AppName {
      override protected def appNameConfiguration: Configuration = Configuration("appName" -> "foo")

      appName shouldBe "foo"
    }

    "return default name if not defined" in new AppName {
      override protected def appNameConfiguration: Configuration = Configuration()

      appName shouldBe "APP NAME NOT SET"
    }
  }

  "AppName object" should {
    "return app name if defined" in {
      AppName(Configuration("appName" -> "foo")).appName shouldBe "foo"
    }

    "return default name if not defined" in {
      AppName(Configuration()).appName shouldBe "APP NAME NOT SET"
    }
  }
}
