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

import com.typesafe.config.Config
import play.api.{Configuration, Mode}
import play.api.Mode.Mode

trait RunModeConfigLoader {

  protected def paths(mode: Mode) = Seq(
    s"govuk-tax.$mode",
    s"$mode"
  )

  def resolveConfig(configuration: Configuration, mode: Mode): Configuration = {

    val allRunModePaths: Seq[String] = {
      for {
        mode <- Mode.values
        path <- paths(mode)
      } yield path
    }.toSeq

    val currentRunModePaths: Seq[String] =
      paths(mode)

    val runModeConfigs: Seq[Configuration] = currentRunModePaths.flatMap(configuration.getConfig)

    val cleanConfig: Config = allRunModePaths.foldLeft(configuration.underlying) {
      case (config, key) =>
        config.withoutPath(key)
    }

    val newConfigEntries: Map[String, Any] = runModeConfigs
      .flatMap(_.entrySet.toSeq)
      .foldLeft(Map.empty[String, Any])(_ + _)

    Configuration(cleanConfig) ++ Configuration(newConfigEntries.toSeq: _*)
  }
}
