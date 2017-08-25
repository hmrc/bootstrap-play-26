package uk.gov.hmrc.play.microservice.config

import play.api.Configuration

case class ControllerConfig(logging: Boolean = true, auditing: Boolean = true)

object ControllerConfig {

  def fromConfig(configuration: Configuration): ControllerConfig = {
    val logging = configuration.getBoolean("needsLogging").getOrElse(true)
    val auditing = configuration.getBoolean("needsAuditing").getOrElse(true)
    ControllerConfig(logging, auditing)
  }
}

case class ControllerConfigs(private val controllers: Map[String, ControllerConfig]) {

  def get(controllerName: String): ControllerConfig =
    controllers.getOrElse(controllerName, ControllerConfig())
}

object ControllerConfigs {

  def fromConfig(configuration: Configuration): ControllerConfigs = {

    val configMap = configuration.getConfig("controllers").map {
      config =>
        config.subKeys.foldLeft(Map.empty[String, ControllerConfig]) {
          case (map, key) =>
            config.getConfig(key)
              .map(ControllerConfig.fromConfig)
              .map(c => map + (key -> c))
              .getOrElse(map)
        }
    }.getOrElse(Map.empty)

    ControllerConfigs(configMap)
  }
}