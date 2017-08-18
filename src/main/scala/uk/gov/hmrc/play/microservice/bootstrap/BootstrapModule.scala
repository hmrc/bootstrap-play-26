package uk.gov.hmrc.play.microservice.bootstrap

import play.api.inject.Module
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.microservice.filters.CacheControlConfig

class BootstrapModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[GraphiteConfiguration].toSelf.eagerly,
    bind[CacheControlConfig].toInstance(CacheControlConfig.fromConfig(configuration))
  )
}