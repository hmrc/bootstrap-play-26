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

package uk.gov.hmrc.play.bootstrap.config

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.Mode.Mode
import play.api.{Configuration, Mode}

trait RunModeConfigLoaderTests extends WordSpec with MustMatchers with OptionValues {

  def resolve(config: (String, Any)*): Configuration

  def aRunModeConfigLoader(): Unit = {

    Seq[Mode => String](
      mode => mode.toString,
      mode => s"govuk-tax.$mode"
    ).foreach {
      prefix =>

        s"add new values that don't have a default, for prefix: ${prefix(Mode.Test)}" in {

          val config = resolve(
            s"${prefix(Mode.Test)}.foo" -> "bar"
          )

          config.getString("foo").value mustEqual "bar"
        }

        s"override a value which has a default, for prefix: ${prefix(Mode.Test)}" in {

          val config = resolve(
            "foo"                       -> "bar",
            s"${prefix(Mode.Test)}.foo" -> "baz"
          )

          config.getString("foo").value mustEqual "baz"
        }

        s"preserve prefixed config, for prefix: ${prefix(Mode.Test)}" in {

          val config = resolve(
            s"${prefix(Mode.Test)}.foo"  -> "bar",
            s"${prefix(Mode.Prod)}.bar"  -> "baz"
          )

          config.getString("foo").value mustEqual "bar"
          config.getString("bar") mustNot be(defined)
          config.getString(s"${prefix(Mode.Test)}.foo").value mustEqual "bar"
          config.getString(s"${prefix(Mode.Prod)}.bar").value mustEqual "baz"
        }
    }

    "override deprecated `govuk-tax` prefix with regular mode prefix" in {

      val config = resolve(
        "govuk-tax.Test.foo"  -> "bar",
        "Test.foo"            -> "baz"
      )

      config.getString("foo").value mustEqual "baz"
    }
  }
}

class RunModeConfigLoaderSpec extends RunModeConfigLoaderTests with RunModeConfigLoader {

  override def resolve(config: (String, Any)*): Configuration =
    resolveConfig(Configuration(config.toSeq: _*), Mode.Test)

  ".resolveConfig" must {
    behave like aRunModeConfigLoader
  }
}
