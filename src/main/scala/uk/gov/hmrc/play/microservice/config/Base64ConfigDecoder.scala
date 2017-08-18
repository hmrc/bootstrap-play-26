package uk.gov.hmrc.play.microservice.config

import com.typesafe.config.{ConfigException, ConfigValue, ConfigValueType}
import org.apache.commons.codec.binary.Base64
import play.api.Configuration

trait Base64ConfigDecoder {

  private val suffix: String = ".base64"

  private def decode(value: String): String =
    new String(Base64.decodeBase64(value))

  private def decode(value: ConfigValue): Option[Any] = {

    val raw = value.unwrapped

    if (value.valueType == ConfigValueType.STRING) {
      Some(decode(raw.asInstanceOf[String]))
    } else {
      None
    }
  }

  private def decode(key: String, value: ConfigValue): (String, Any) = {
    decode(value).map(key.dropRight(suffix.length) -> _).getOrElse {
      throw new ConfigException.BadValue(
        value.origin,
        key,
        "only strings can be Base64 decoded"
      )
    }
  }

  protected def decodeConfig(configuration: Configuration): Configuration = {

    val base64Conf = configuration.entrySet.filter {
      case (key, _) =>
        key.endsWith(suffix)
    }

    val newConfig = base64Conf.foldLeft(Map.empty[String, Any]) {
      case (config, (key, value)) =>
        config + decode(key, value)
    }

    val nonBase64Conf = base64Conf.toMap.keys.foldLeft(configuration.underlying) {
      case (config, key) =>
        config.withoutPath(key.dropRight(suffix.length))
    }

    Configuration(newConfig.toSeq: _*) ++ Configuration(nonBase64Conf)
  }
}
