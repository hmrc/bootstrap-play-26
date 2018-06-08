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

import org.scalatest.{Matchers, WordSpecLike}

class ControllerConfigTest extends WordSpecLike with Matchers {

  import com.typesafe.config.{Config, ConfigFactory}
  import net.ceedubs.ficus.Ficus._

  val config = ConfigFactory.parseString(
    """
      |controllers {
      |  uk.gov.hmrc.play.controllers.HealthCheck {
      |    needsAuditing = false
      |    needsLogging = false
      |    needsAuth = false
      |  }
      |  com.kenshoo.play.metrics.MetricsController {
      |    needsAuditing = false
      |  }
      |}
    """.stripMargin)

  val cc = new ControllerConfig {
    lazy val controllerConfigs = config.as[Config]("controllers")
  }

  "controller config" should {
    "say that uk.gov.hmrc.play.controllers.HealthCheck does not need auditing" in {
      cc.paramsForController("uk.gov.hmrc.play.controllers.HealthCheck").needsAuditing shouldBe false
    }
    "say that uk.gov.hmrc.play.controllers.HealthCheck does not need logging" in {
      cc.paramsForController("uk.gov.hmrc.play.controllers.HealthCheck").needsLogging shouldBe false
    }
    "say that uk.gov.hmrc.play.controllers.HealthCheck does not need auth" in {
      cc.paramsForController("uk.gov.hmrc.play.controllers.HealthCheck").needsAuth shouldBe false
    }

    "return a config for com.kenshoo.play.metrics.MetricsController" in {
      cc.paramsForController("com.kenshoo.play.metrics.MetricsController") shouldNot be(None)
    }

    "say that com.kenshoo.play.metrics.MetricsController does not need auditing" in {
      cc.paramsForController("com.kenshoo.play.metrics.MetricsController").needsAuditing shouldBe false
    }
    "say that com.kenshoo.play.metrics.MetricsController needs logging" in {
      cc.paramsForController("com.kenshoo.play.metrics.MetricsController").needsLogging shouldBe true
    }
    "say that com.kenshoo.play.metrics.MetricsController needs auth" in {
      cc.paramsForController("com.kenshoo.play.metrics.MetricsController").needsAuth shouldBe true
    }
  }
}
