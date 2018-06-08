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

import scala.concurrent.duration.Duration

trait ServicesConfig extends RunMode {

  protected lazy val rootServices = "microservice.services"
  protected lazy val services = s"$env.microservice.services"

//  @deprecated("The 'govuk-tax' is an unnecessary level of configuration please use ServicesConfig.services", "24.11.14")
  protected lazy val playServices = s"govuk-tax.$env.services"

  protected lazy val defaultProtocol: String =
    runModeConfiguration.getString(s"$rootServices.protocol")
    .getOrElse(runModeConfiguration.getString(s"$services.protocol")
      .getOrElse("http"))

  protected def config(serviceName: String) =
    runModeConfiguration.getConfig(s"$rootServices.$serviceName")
      .getOrElse(runModeConfiguration.getConfig(s"$services.$serviceName")
      .getOrElse(runModeConfiguration.getConfig(s"$playServices.$serviceName")
      .getOrElse(throw new IllegalArgumentException(s"Configuration for service $serviceName not found"))))

  def baseUrl(serviceName: String) = {
    val protocol = getConfString(s"$serviceName.protocol",defaultProtocol)
    val host = getConfString(s"$serviceName.host", throw new RuntimeException(s"Could not find config $serviceName.host"))
    val port = getConfInt(s"$serviceName.port", throw new RuntimeException(s"Could not find config $serviceName.port"))
    s"$protocol://$host:$port"
  }

  def getConfString(confKey: String, defString: => String) = {
    runModeConfiguration.getString(s"$rootServices.$confKey").
      getOrElse(runModeConfiguration.getString(s"$services.$confKey").
      getOrElse(runModeConfiguration.getString(s"$playServices.$confKey").
      getOrElse(defString)))
  }

  def getConfInt(confKey: String, defInt: => Int) = {
    runModeConfiguration.getInt(s"$rootServices.$confKey").
      getOrElse(runModeConfiguration.getInt(s"$services.$confKey").
      getOrElse(runModeConfiguration.getInt(s"$playServices.$confKey").
      getOrElse(defInt)))
  }

  def getConfBool(confKey: String, defBool: => Boolean) = {
    runModeConfiguration.getBoolean(s"$rootServices.$confKey").
      getOrElse(runModeConfiguration.getBoolean(s"$services.$confKey").
      getOrElse(runModeConfiguration.getBoolean(s"$playServices.$confKey").
      getOrElse(defBool)))
  }

  def getConfDuration(confKey: String, defDur: => Duration) =
    runModeConfiguration.getString(s"$rootServices.$confKey")
      .orElse(runModeConfiguration.getString(s"$services.$confKey"))
      .orElse(runModeConfiguration.getString(s"$playServices.$confKey")) match {
        case Some(s) => Duration.create(s)
        case None => defDur
      }

  def getInt(key: String) = runModeConfiguration.getInt(key).getOrElse(configNotFoundError(key))

  def getString(key: String) = runModeConfiguration.getString(key).getOrElse(configNotFoundError(key))

  def getBoolean(key: String) = runModeConfiguration.getBoolean(key).getOrElse(configNotFoundError(key))

  def getDuration(key: String) = runModeConfiguration.getString(key).map(Duration.create).getOrElse(configNotFoundError(key))

  private def configNotFoundError(key: String) = throw new RuntimeException(s"Could not find config key '$key'")

}
