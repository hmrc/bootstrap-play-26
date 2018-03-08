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

import javax.inject.Inject

import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}

class MDCInstance @Inject()() {
  def put(k: String, v: String): Unit =
    MDC.put(k, v)
}

class MDCLoggingSetup @Inject()(
  override val configuration: Configuration,
  mdc: MDCInstance,
  environment: Environment
) extends AppName {

  val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")

  mdc.put("appName", appName)
  loggerDateFormat.foreach(mdc.put("logger.json.dateformat", _))

  Logger.info(s"Starting frontend: $appName, in mode: ${environment.mode}")
}
