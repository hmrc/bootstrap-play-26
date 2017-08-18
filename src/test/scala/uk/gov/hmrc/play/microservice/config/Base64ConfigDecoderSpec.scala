package uk.gov.hmrc.play.microservice.config

import com.typesafe.config.ConfigException
import org.apache.commons.codec.binary.Base64
import org.scalatest.{MustMatchers, WordSpec}
import play.api.Configuration

trait Base64ConfigDecoderTests extends WordSpec with MustMatchers {

  def decode(config: (String, Any)*): Configuration

  def aBase64Decoder: Unit = {

    val quux = Base64.encodeBase64String("quux".getBytes())

    val config = decode(
      "foo" -> "bar",
      "womble" -> 7331,
      "baz.base64" -> quux
    )

    "not replace non-encoded values" in {
      config.getString("foo") mustBe Some("bar")
      config.getInt("womble") mustBe Some(7331)
    }

    "decode encoded values" in {
      config.getString("baz") mustBe Some("quux")
    }

    "throw an exception when trying to decode a non-string value" in {

      val exception = intercept[ConfigException.BadValue] {
        decode(
          "spoon.base64" -> 1337
        )
      }

      exception.getMessage mustEqual "hardcoded value: Invalid value at 'spoon.base64': only strings can be Base64 decoded"
    }
  }
}

class Base64ConfigDecoderSpec extends Base64ConfigDecoderTests with Base64ConfigDecoder {

  override def decode(config: (String, Any)*): Configuration =
    decodeConfig(Configuration(config.toSeq: _*))

  ".decode" must {
    behave like aBase64Decoder
  }
}
