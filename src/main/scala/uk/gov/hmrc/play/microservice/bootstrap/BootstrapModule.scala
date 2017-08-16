package uk.gov.hmrc.play.microservice.bootstrap

import com.google.inject.AbstractModule

class BootstrapModule extends AbstractModule {
  def configure() = {

    bind(classOf[GraphiteConfiguration]).to(classOf[GraphiteConfiguration]).asEagerSingleton()

  }
}