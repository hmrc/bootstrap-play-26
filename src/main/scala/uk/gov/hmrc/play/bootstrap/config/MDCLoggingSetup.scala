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

import javax.inject.{Inject, Named}
import org.slf4j.MDC
import play.api.{Configuration, Logger, Mode}

class MDCLoggingSetup(
  configuration: Configuration,
  mode: Mode,
  appName: String,
  mdcPut: (String, String) => Unit
) {

  @Inject() def this(configuration: Configuration, mode: Mode, @Named("appName") appName: String) =
    this(configuration, mode, appName, MDC.put)

  val loggerDateFormat: Option[String] = configuration.getOptional[String]("logger.json.dateformat")

  mdcPut("appName", appName)
  loggerDateFormat.foreach(mdcPut("logger.json.dateformat", _))

  Logger.info(s"Starting frontend: $appName, in mode: $mode")
}
