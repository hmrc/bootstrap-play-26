package uk.gov.hmrc.play.microservice.bootstrap

import play.api.inject.Module
import play.api.{Configuration, Environment}

class BootstrapModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[GraphiteConfiguration].toSelf.eagerly
  )
}