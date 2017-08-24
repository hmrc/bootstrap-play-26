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

package uk.gov.hmrc.play.microservice.bootstrap

import java.net.InetSocketAddress
import javax.inject.{Inject, Singleton}

import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import org.slf4j.MDC
import play.api.{Application, Configuration, Logger}

import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

@Singleton
class GraphiteConfiguration @Inject()(configuration: Configuration, app: Application) {

  val appName: String = configuration.getString("appName").getOrElse("APP NAME NOT SET")

  val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")

  val graphiteConfig: Configuration = configuration.getConfig("microservice.metrics.graphite").getOrElse(throw new Exception("No configuration for microservice.metrics.graphite found"))

  val metricsPluginEnabled: Boolean = configuration.getBoolean("metrics.enabled").getOrElse(false)

  val graphitePublisherEnabled: Boolean = graphiteConfig.getBoolean("enabled").getOrElse(false)

  val graphiteEnabled: Boolean = metricsPluginEnabled && graphitePublisherEnabled

  val registryName: String = configuration.getString("metrics.name").getOrElse("default")

  Logger.info(s"Starting frontend : $appName : in mode : ${app.mode}")
  MDC.put("appName", appName)
  loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))

  if (graphiteEnabled) startGraphite()

  private def startGraphite(): Unit = {
    Logger.info("Graphite metrics enabled, starting the reporter")

    val graphite = new Graphite(new InetSocketAddress(
      graphiteConfig.getString("host").getOrElse("graphite"),
      graphiteConfig.getInt("port").getOrElse(2003)))

    val prefix = graphiteConfig.getString("prefix").getOrElse("play.cbcr")

    val reporter = GraphiteReporter.forRegistry(
      SharedMetricRegistries.getOrCreate(registryName))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(graphiteConfig.getLong("interval").getOrElse(10L), SECONDS)
  }
}
