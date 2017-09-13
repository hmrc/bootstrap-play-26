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

import play.api.{Configuration, Logger}

import scala.language.implicitConversions

trait BaseUrl {

  protected def configuration: Configuration

  private lazy val legacyConfiguration: Option[Configuration] = configuration.getConfig("services")

  private lazy val root: Configuration = {

    legacyConfiguration.foreach {
      _ =>
        Logger.warn("`services` configuration is deprecated, use `microservice.services` instead")
    }

    configuration.getConfig("microservice.services") orElse legacyConfiguration
  }.getOrElse(Configuration.empty)

  private lazy val defaultProtocol: String =
    root.getString("protocol").getOrElse("http")

  def baseUrl(serviceName: String): String = {

    val config    = root.underlying.getConfig(serviceName)

    val protocol  = Configuration(config).getString("protocol").getOrElse(defaultProtocol)
    val host      = config.getString("host")
    val port      = config.getString("port")

    s"$protocol://$host:$port"
  }
}
