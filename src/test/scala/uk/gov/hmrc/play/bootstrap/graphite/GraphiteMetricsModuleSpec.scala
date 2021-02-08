/*
 * Copyright 2021 HM Revenue & Customs
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

import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.kenshoo.play.metrics._
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import play.api.Configuration
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class GraphiteMetricsModuleSpec
    extends FreeSpec
    with MustMatchers
    with BeforeAndAfterEach
    with PropertyChecks
    with GivenWhenThen {

  def app: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .bindings(new GraphiteMetricsModule)

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  def setupInjector(configuration: Configuration): Injector =
    new GuiceApplicationBuilder()
      .bindings(new GraphiteMetricsModule)
      .configure(configuration)
      .build()
      .injector

  "if 'metrics.enabled' set to false" in {

    SharedMetricRegistries.clear()
    val injector: Injector = setupInjector(Configuration("metrics.enabled" -> false))

    Then("kenshoo metrics are disabled")
    injector.instanceOf[MetricsFilter] mustBe a[DisabledMetricsFilter]

  }

  for (prefix <- Seq("", "Test.")) {

    s"if missing kenshoo metrics enabled, but '${prefix}microservice.metrics.graphite.enabled' missing" in {

      SharedMetricRegistries.clear()
      val injector: Injector = setupInjector(Configuration("metrics.enabled" -> "true"))

      Then("kensho metrics are enabled")
      injector.instanceOf[MetricsFilter] mustBe a[MetricsFilterImpl]
      injector.instanceOf[Metrics] mustBe a[MetricsImpl]

      Then("graphite reporting in disabled")
      injector.instanceOf[GraphiteReporting] mustBe a[DisabledGraphiteReporting]

    }

    s"property testing with prefix [$prefix]" in {

      forAll { (kenshooEnabled: Boolean, graphiteEnabled: Boolean) =>
        SharedMetricRegistries.clear()

        val configuration = Configuration("metrics.enabled" -> kenshooEnabled) ++
          (if (graphiteEnabled) {
             Configuration(
               s"${prefix}microservice.metrics.graphite.enabled" -> true,
               s"${prefix}microservice.metrics.graphite.host"    -> "test",
               s"${prefix}microservice.metrics.graphite.port"    -> "9999",
               "appName"                                         -> "test"
             )
           } else {
             Configuration(s"${prefix}microservice.metrics.graphite.enabled" -> false)
           })

        val injector: Injector = setupInjector(configuration)

        if (kenshooEnabled) {
          //enabled kenshoo metrics filter included
          injector.instanceOf[MetricsFilter] mustBe a[MetricsFilterImpl]
        }

        if (!kenshooEnabled) {
          //disabled kenshoo metrics filter included
          injector.instanceOf[MetricsFilter] mustBe a[DisabledMetricsFilter]

          //there is a binding to graphite disabledMetrics
          injector.instanceOf[Metrics] mustBe a[DisabledMetrics]
        }

        //there is a binding to graphite's metricsimpl or graphitemetricsimpl
        if (kenshooEnabled) {
          injector.instanceOf[Metrics] mustBe a[MetricsImpl]
          injector.instanceOf[MetricFilter] mustEqual MetricFilter.ALL
        }

        if (kenshooEnabled && graphiteEnabled) {
          //there is an enabled graphite reporter
          injector.instanceOf[GraphiteReporting] mustBe a[EnabledGraphiteReporting]
        }

        if (!kenshooEnabled || !graphiteEnabled) {
          //there is a disabled graphite reporter
          injector.instanceOf[GraphiteReporting] mustBe a[DisabledGraphiteReporting]
        }

      }
    }

  }

}
