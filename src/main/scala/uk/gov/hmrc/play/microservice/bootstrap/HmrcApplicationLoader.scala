package uk.gov.hmrc.play.microservice.bootstrap

import play.api.ApplicationLoader.Context
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import uk.gov.hmrc.play.microservice.config.Base64ConfigDecoder

class HmrcApplicationLoader extends GuiceApplicationLoader with Base64ConfigDecoder {

  override def builder(context: Context): GuiceApplicationBuilder = {
    initialBuilder
      .loadConfig(decodeConfig(context.initialConfiguration))
  }
}
