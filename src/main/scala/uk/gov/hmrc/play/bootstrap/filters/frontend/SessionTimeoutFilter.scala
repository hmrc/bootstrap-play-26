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

import akka.stream.Materializer
import org.joda.time.{DateTime, DateTimeZone, Duration}
import play.api.{Configuration, Play}
import play.api.http.HeaderNames.COOKIE
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Filter that manipulates session data if 'ts' session field is older than configured timeout.
  *
  * If the 'ts' has expired, we wipe the session, and update the 'ts'.
  * If the 'ts' doesn't exist, or is invalid, we just wipe the authToken.
  *
  * This filter clears data on the incoming request, so that the controller does not receive any session information.
  * It also changes the SET-COOKIE header for the outgoing request, so that the browser knows the session has expired.
  *
  * A white-list of session values are omitted from this process.
  *
  * @param clock           function that supplies the current [[DateTime]]
  * @param timeoutDuration how long an untouched session should be considered valid for
  */
class SessionTimeoutFilter(clock: () => DateTime = () => DateTime.now(DateTimeZone.UTC),
                           timeoutDuration: Duration,
                           additionalSessionKeysToKeep: Set[String] = Set.empty,
                           onlyWipeAuthToken: Boolean = false,
                           override val mat: Materializer) extends Filter {

  val authRelatedKeys = Seq(authToken, token, userId)

  private def wipeFromSession(session: Session, keys: Seq[String]): Session = keys.foldLeft(session)((s, k) => s - k)

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val updateTimestamp: (Result) => Result =
      result => result.addingToSession(lastRequestTimestamp -> clock().getMillis.toString)(rh)

    val wipeAllFromSessionCookie: (Result) => Result =
      result => result.withSession(preservedSessionData(result.session(rh)): _*)

    val wipeAuthRelatedKeysFromSessionCookie: (Result) => Result =
      result => result.withSession(wipeFromSession(result.session(rh), authRelatedKeys))

    val wipeTimestampFromSessionCookie: (Result) => Result =
      result => result.withSession(result.session(rh) - lastRequestTimestamp)

    val timestamp = rh.session.get(lastRequestTimestamp)

    (timestamp.flatMap(timestampToDatetime) match {
      case Some(ts) if hasExpired(ts) && onlyWipeAuthToken =>
        f(wipeAuthRelatedKeys(rh))
          .map(wipeAuthRelatedKeysFromSessionCookie)
      case Some(ts) if hasExpired(ts) =>
        f(wipeSession(rh))
          .map(wipeAllFromSessionCookie)
      case _ =>
        f(rh)
    }).map(updateTimestamp)
  }

  private def timestampToDatetime(timestamp: String): Option[DateTime] =
    try {
      Some(new DateTime(timestamp.toLong, DateTimeZone.UTC))
    } catch {
      case e: NumberFormatException => None
    }

  private def hasExpired(timestamp: DateTime): Boolean = {
    val timeOfExpiry = timestamp plus timeoutDuration
    clock() isAfter timeOfExpiry
  }

  private def wipeSession(requestHeader: RequestHeader): RequestHeader = {
    val sessionMap: Map[String, String] = preservedSessionData(requestHeader.session).toMap
    mkRequest(requestHeader, Session.deserialize(sessionMap))
  }

  private def wipeAuthRelatedKeys(requestHeader: RequestHeader): RequestHeader = {
    mkRequest(requestHeader, wipeFromSession(requestHeader.session, authRelatedKeys))
  }

  private def mkRequest(requestHeader: RequestHeader, session: Session): RequestHeader = {
    val wipedSessionCookie = Session.encodeAsCookie(session)
    val otherCookies = requestHeader.cookies.filterNot(_.name == wipedSessionCookie.name).toSeq
    val wipedHeaders = requestHeader.headers.replace(COOKIE -> Cookies.encodeCookieHeader(Seq(wipedSessionCookie) ++ otherCookies))
    requestHeader.copy(headers = wipedHeaders)
  }

  private def preservedSessionData(session: Session): Seq[(String, String)] = for {
    key <- (SessionTimeoutFilter.whitelistedSessionKeys ++ additionalSessionKeysToKeep).toSeq
    value <- session.get(key)
  } yield key -> value

}

object SessionTimeoutFilter {
  val whitelistedSessionKeys: Set[String] = Set(
    lastRequestTimestamp, // the timestamp that this filter manages
    redirect, // a redirect used by some authentication provider journeys
    loginOrigin, // the name of a service that initiated a login
    "Csrf-Token", // the Play default name for a header that contains the CsrfToken value (here only in case it is being misused in tests)
    "csrfToken", // the Play default name for the CsrfToken value within the Play Session)
    authProvider // a deprecated value that indicates what authentication provider was used for the session - may be used to handle default redirects on failed logins
  )

  @deprecated("Use dependency injection", "8.0.0")
  def apply(configuration: Configuration): SessionTimeoutFilter = {
    val defaultTimeout = Duration.standardMinutes(15)
    val timeoutDuration = configuration
      .getLong("session.timeoutSeconds")
      .map(Duration.standardSeconds)
      .getOrElse(defaultTimeout)

    val wipeIdleSession = configuration
      .getBoolean("session.wipeIdleSession")
      .getOrElse(true)

    val additionalSessionKeysToKeep = configuration
      .getStringSeq("session.additionalSessionKeysToKeep")
      .getOrElse(Seq.empty).toSet

    new SessionTimeoutFilter(
      timeoutDuration = timeoutDuration,
      additionalSessionKeysToKeep = additionalSessionKeysToKeep,
      onlyWipeAuthToken = !wipeIdleSession,
      mat = Play.current.materializer
    )
  }
}
