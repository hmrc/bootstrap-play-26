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

import java.security.MessageDigest
import java.util.UUID

import org.apache.commons.codec.binary.Base64

import scala.util.Try

/**
 * The DeviceId is a long lived cookie which represents a digital signature composed of a UUID, timestamp in milliseconds and a hash.
 *
 * The format of the cookie 'mdtpdi' is...
 *
 *    mdtpdi#UUID#TIMESTAMP_hash
 *
 * The legacy deviceId had the format below. The legacy cookie is automatically converted to a new cookie...
 *
 *    UUID_hash
 *
 * Note the above hash is a one way hash of the value preceding the "_".
 *
 */

case class DeviceId(uuid: String, timestamp:Option[Long], hash: String) {

  def value = DeviceId.generateSignature(this)
}

object DeviceId {
  final val Token1 = "#"
  final val Token2 = "_"
  final val TenYears = 315360000
  final val MdtpDeviceId = "mdtpdi"


  def generateSignature(deviceId:DeviceId) =
    deviceId.timestamp.fold(s"${deviceId.uuid}$Token2${deviceId.hash}")(time => s"$MdtpDeviceId$Token1${deviceId.uuid}$Token1$time$Token2${deviceId.hash}")

  def generateHash(uuid:String, timestamp:Option[Long], secret:String) = {
    val oneWayHash = timestamp.fold(uuid)(time => s"$MdtpDeviceId$Token1$uuid$Token1$time")
    val digest = MessageDigest.getInstance("MD5").digest((oneWayHash + secret).getBytes)
    new String(Base64.encodeBase64(digest))
  }

  def deviceIdHashIsValid(hash:String, uuid:String, timestamp:Option[Long], secret:String, previousSecrets:Seq[String]) = {
    val secrets = Seq(secret) ++ previousSecrets
    val hashChecker = secrets.map(item => () => hash == generateHash(uuid, timestamp, item)).toStream
    hashChecker.map(_()).collectFirst { case true => true }.getOrElse(false)
  }

  def from(value: String, secret:String, previousSecrets:Seq[String]) = {
    def isValidPrefix(prefix:String) = prefix == MdtpDeviceId

    def isValid(prefix:String, uuid:String, timestamp:String, hash:String) =
      isValidPrefix(prefix) && validUuid(uuid) && validLongTime(timestamp) && deviceIdHashIsValid(hash, uuid, Some(timestamp.toLong), secret, previousSecrets)

    def isValidLegacy(uuid:String, hash:String) = validUuid(uuid) && deviceIdHashIsValid(hash, uuid, None, secret, previousSecrets)

    value.split("(#)|(_)") match {
      case Array(prefix, uuid, timestamp, hash) if isValid(prefix, uuid, timestamp, hash) =>
        Some(DeviceId(uuid, Some(timestamp.toLong), hash))

      case Array(uuid, hash) if isValidLegacy(uuid, hash) =>
        Some(DeviceId(uuid, None, hash))

      case _ => None
    }
  }

  private def validUuid(uuid: String) = Try {UUID.fromString(uuid)}.isSuccess

  private def validLongTime(timestamp: String) = Try {timestamp.toLong}.isSuccess

}
