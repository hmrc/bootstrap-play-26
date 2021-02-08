/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpec}
import play.api.{Configuration, PlayException}

class DeprecatedConfigCheckerSpec extends WordSpec with Matchers {

  "DeprecatedConfigChecker" should {
    "throw exception when obsolete keys are used" in {
      val config = Configuration(
        "bootstrap.configuration.failOnObsoleteKeys" -> true,
        "httpHeadersWhitelist"                       -> List(""),
        "bootstrap.filters.whitelist.enabled"        -> true,
        "bootstrap.filters.whitelist.destination"    -> List(""),
        "bootstrap.filters.whitelist.excluded"       -> List(""),
        "bootstrap.filters.whitelist.ips"            -> List("")
      )

      assertThrows[PlayException] {
        new DeprecatedConfigChecker(config)
      }
    }

    "detect useage of deprecated keys" in {
      val r = scala.util.Random
      val allDeprecatedConfig = Seq(
        "httpHeadersWhitelist"                       -> List(""),
        "bootstrap.filters.whitelist.enabled"        -> true,
        "bootstrap.filters.whitelist.destination"    -> List(""),
        "bootstrap.filters.whitelist.excluded"       -> List(""),
        "bootstrap.filters.whitelist.ips"            -> List("")
      )
      val deprecatedConfig = r.shuffle(allDeprecatedConfig).take(r.nextInt(allDeprecatedConfig.length))

      val config = Configuration(
        (("bootstrap.configuration.failOnObsoleteKeys" -> false) +: deprecatedConfig) :_ *
      )

      val deprecatedConfigChecker = new DeprecatedConfigChecker(config)
      deprecatedConfigChecker.errs shouldBe deprecatedConfigChecker.deprecatedKeys.filter { case (d, _) => deprecatedConfig.exists(_._1 == d) }
    }
  }
}
