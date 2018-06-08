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

import scala.util.matching.Regex

case class ControllerParams(needsLogging: Boolean = true, needsAuditing: Boolean = true, needsAuth: Boolean = true)


trait ControllerConfig {

  import com.typesafe.config.Config
  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.ValueReader
  import net.ceedubs.ficus.readers.StringReader

  def controllerConfigs: Config

  private implicit val regexValueReader: ValueReader[Regex] = StringReader.stringValueReader.map(_.r)

  private implicit val controllerParamsReader = ValueReader.relative[ControllerParams] { config =>
    ControllerParams(
      needsLogging = config.getAs[Boolean]("needsLogging").getOrElse(true),
      needsAuditing = config.getAs[Boolean]("needsAuditing").getOrElse(true),
      needsAuth = config.getAs[Boolean]("needsAuth").getOrElse(true)
    )
  }

  def paramsForController(controllerName: String): ControllerParams = {
    controllerConfigs.as[Option[ControllerParams]](controllerName).getOrElse(ControllerParams())
  }
}
