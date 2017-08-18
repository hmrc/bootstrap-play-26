package uk.gov.hmrc.play.microservice.bootstrap

import play.api.{ApplicationLoader, Configuration, Environment}
import uk.gov.hmrc.play.microservice.config.Base64ConfigDecoderTests

class HmrcApplicationLoaderSpec extends Base64ConfigDecoderTests {

  val loader = new HmrcApplicationLoader()

  override def decode(config: (String, Any)*): Configuration = {

    val context = {
      val ctx = ApplicationLoader.createContext(Environment.simple())
      ctx.copy(initialConfiguration = ctx.initialConfiguration ++ Configuration(config: _*))
    }

    loader.load(context).configuration
  }

  ".load" must {
    behave like aBase64Decoder
  }
}