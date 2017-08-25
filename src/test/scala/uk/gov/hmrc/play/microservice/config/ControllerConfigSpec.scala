package uk.gov.hmrc.play.microservice.config

import org.scalatest.{MustMatchers, WordSpec}
import play.api.Configuration

class ControllerConfigSpec extends WordSpec with MustMatchers {

  "ControllerConfig.fromConfig" must {

    "return defaults when no config is found" in {

      val config = ControllerConfig.fromConfig(Configuration())

      config.auditing mustBe true
      config.logging mustBe true
    }

    "return defaults when config is set to defaults" in {

      val config = ControllerConfig.fromConfig(Configuration("needsAuditing" -> true, "needsLogging" -> true))

      config.auditing mustBe true
      config.logging mustBe true
    }

    "return all `false` when config is set to that" in {

      val config = ControllerConfig.fromConfig(Configuration("needsAuditing" -> false, "needsLogging" -> false))

      config.auditing mustBe false
      config.logging mustBe false
    }
  }

  "ControllerConfigs.fromConfig" must {

    val controllerConfigs = ControllerConfigs.fromConfig(Configuration(
      "controllers.foo.needsAuditing" -> false,
      "controllers.foo.needsLogging" -> false
    ))

    "return loaded configuration" in {

      val config = controllerConfigs.get("foo")

      config.auditing mustBe false
      config.logging mustBe false
    }

    "return default configuration for missing controllers" in {

      val config = controllerConfigs.get("bar")

      config.auditing mustBe true
      config.logging mustBe true
    }
  }
}
