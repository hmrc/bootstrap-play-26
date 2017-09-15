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

import com.typesafe.config.ConfigObject
import play.api.Configuration

case class ControllerConfig(logging: Boolean = true, auditing: Boolean = true)

object ControllerConfig {

  def fromConfig(configuration: Configuration): ControllerConfig = {
    val logging = configuration.getBoolean("needsLogging").getOrElse(true)
    val auditing = configuration.getBoolean("needsAuditing").getOrElse(true)
    ControllerConfig(logging, auditing)
  }
}

case class ControllerConfigs(private val controllers: Map[String, ControllerConfig]) {

  def get(controllerName: String): ControllerConfig =
    controllers.getOrElse(controllerName, ControllerConfig())
}

object ControllerConfigs {

  def fromConfig(configuration: Configuration): ControllerConfigs = {

    val configMap = (
      for (
      configs <- configuration.getConfig("controllers").toSeq;
      key <- configs.subKeys;
      entryForController <- readCompositeValue(configs, key);
      parsedEntryForController = ControllerConfig.fromConfig(entryForController)
    ) yield (key, parsedEntryForController)
      ).toMap

    ControllerConfigs(configMap)
  }

  private def readCompositeValue(c : Configuration, key : String) : Option[Configuration] = {
    try {
      if (c.underlying.hasPathOrNull(key)) {
        c.underlying.getValue(key) match {
          case c : ConfigObject => Some(Configuration(c.toConfig))
          case _ => None
        }
      } else None
    }
  }
}