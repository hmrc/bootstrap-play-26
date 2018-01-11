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

package uk.gov.hmrc.play.bootstrap.graphite

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.kenshoo.play.metrics._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.RunMode

class GraphiteMetricsModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val defaultBindings: Seq[Binding[_]] = Seq(
      // Note: `MetricFilter` rather than `MetricsFilter`
      bind[MetricFilter].toInstance(MetricFilter.ALL).eagerly
    )

    val kenshoBindings : Seq[Binding[_]] =
      if (kenshoMetricsEnabled(configuration)) {
        Seq(
          bind[MetricsFilter].to[MetricsFilterImpl].eagerly,
          bind[Metrics].to[MetricsImpl].eagerly)
      } else {
        Seq(
          bind[MetricsFilter].to[DisabledMetricsFilter].eagerly,
          bind[Metrics].to[DisabledMetrics].eagerly)
      }

    val graphiteConfiguration = extractGraphiteConfiguration(environment, configuration)

    val graphiteBindings: Seq[Binding[_]] =
      if (kenshoMetricsEnabled(configuration) && graphitePublisherEnabled(graphiteConfiguration)) {
        Seq(
          bind[GraphiteProviderConfig].toInstance(GraphiteProviderConfig.fromConfig(graphiteConfiguration)),
          bind[GraphiteReporterProviderConfig].toInstance(GraphiteReporterProviderConfig.fromConfig(configuration, graphiteConfiguration)),
          bind[Graphite].toProvider[GraphiteProvider],
          bind[GraphiteReporter].toProvider[GraphiteReporterProvider],
          bind[GraphiteReporting].to[EnabledGraphiteReporting].eagerly
        )
      } else {
        Seq(
          bind[GraphiteReporting].to[DisabledGraphiteReporting].eagerly
        )
      }

    defaultBindings ++ graphiteBindings ++ kenshoBindings
  }

  private def kenshoMetricsEnabled(rootConfiguration: Configuration) =
    rootConfiguration.getBoolean("metrics.enabled").getOrElse(false)

  private def graphitePublisherEnabled(graphiteConfiguration: Configuration) =
    graphiteConfiguration.getBoolean("enabled").getOrElse(false)

  private def extractGraphiteConfiguration(environment: Environment, configuration: Configuration): Configuration = {
    val env = RunMode(environment.mode, configuration).env
    configuration.getConfig(s"$env.microservice.metrics.graphite")
      .orElse(configuration.getConfig("microservice.metrics.graphite"))
      .getOrElse(Configuration())
  }
}
