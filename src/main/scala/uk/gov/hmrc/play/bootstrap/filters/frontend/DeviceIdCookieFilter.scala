/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.filters.frontend

import org.apache.commons.codec.binary.Base64
import play.api.Play.current
import play.api.{Logger, Play}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.filters.MicroserviceFilterSupport

class DeviceIdCookieFilter(val appName: String, val auditConnector: AuditConnector) extends DeviceIdFilter
  with MicroserviceFilterSupport {

  final val currentSecret = "cookie.deviceId.secret"
  final val previousSecret = "cookie.deviceId.previous.secret"
  final val message = "Missing required configuration entry for deviceIdFilter :"

  override lazy val secret: String = Play.configuration.getString(currentSecret).getOrElse {
    Logger.error(s"$message $currentSecret")
    throw new SecurityException(s"$message $currentSecret")
  }

  override lazy val previousSecrets = {
    (for {
      encoded <- Play.current.configuration.getStringSeq(previousSecret)
      stringList <- Some(encoded.map(item => new String(Base64.decodeBase64(item))))
    } yield (stringList)).getOrElse(Seq.empty)
  }

}

object DeviceIdCookieFilter {
  def apply(appName: String, auditConnector: AuditConnector) = new DeviceIdCookieFilter(appName, auditConnector)
}
