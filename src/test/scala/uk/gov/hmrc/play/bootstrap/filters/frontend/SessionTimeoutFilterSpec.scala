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

import org.joda.time.{DateTime, Duration}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerTest
import play.api.mvc.{RequestHeader, _}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.http.SessionKeys._
import uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter._

import scala.concurrent.Future

class SessionTimeoutFilterSpec extends WordSpecLike with Matchers with ScalaFutures with OneAppPerTest {

  "SessionTimeoutFilter" should {

    val now = new DateTime(2017, 1, 12, 14, 56)
    val timeoutDuration = Duration.standardMinutes(1)
    val clock = () => now
    val filter = new SessionTimeoutFilter(clock, timeoutDuration,
      additionalSessionKeysToKeep = Set("whitelisted"),
      onlyWipeAuthToken = false
    )

    "strip non-whitelist session variables from request if timestamp is old" in {
      val timestamp = now.minusMinutes(5).getMillis.toString
      implicit val rh = exampleRequest.withSession(
        lastRequestTimestamp -> timestamp,
        authToken -> "a-token",
        userId -> "some-userId",
        "whitelisted" -> "whitelisted")

      filter.apply { req =>
        req.session should onlyContainWhitelistedKeys(Set("whitelisted"))
        req.session.get(lastRequestTimestamp) shouldBe Some(timestamp)
        req.session.get("whitelisted") shouldBe Some("whitelisted")
        Future.successful(Results.Ok)
      }(rh)
    }

    "strip non-whitelist session variables from result if timestamp is old" in {
      val timestamp = now.minusMinutes(5).getMillis.toString
      implicit val rh = exampleRequest.withSession(lastRequestTimestamp -> timestamp, loginOrigin -> "gg", authToken -> "a-token", "whitelisted" -> "whitelisted")

      val result = filter(successfulResult)(rh)

      result.futureValue.session should onlyContainWhitelistedKeys(Set("whitelisted"))
      result.futureValue.session.get(loginOrigin) shouldBe Some("gg")
      result.futureValue.session.get("whitelisted") shouldBe Some("whitelisted")
    }

    "pass through all session values if timestamp is recent" in {
      val timestamp = now.minusSeconds(5).getMillis.toString
      implicit val rh = exampleRequest.withSession(lastRequestTimestamp -> timestamp, authToken -> "a-token", "custom" -> "custom")

      val result = filter.apply { req =>
        req.session.get("custom") shouldBe Some("custom")
        Future.successful(Results.Ok)
      }(rh)

      result.futureValue.session.get("custom") shouldBe Some("custom")
    }

    "create timestamp if it's missing" in {
      implicit val rh = exampleRequest.withSession(
        authToken -> "a-token",
        token -> "another-token",
        userId -> "a-userId",
        "custom" -> "custom")

      val result = filter.apply { req =>
        req.session.get(authToken) shouldBe Some("a-token")
        req.session.get(userId) shouldBe Some("a-userId")
        req.session.get(token) shouldBe Some("another-token")
        req.session.get("custom") shouldBe Some("custom")
        req.session.get(lastRequestTimestamp) shouldBe None
        Future.successful(Results.Ok)
      }(rh)

      result.futureValue.session.get(lastRequestTimestamp) shouldBe Some(now.getMillis.toString)
    }

    "strip only auth-related keys if timestamp is old, and onlyWipeAuthToken == true" in {
      val oldTimestamp = now.minusMinutes(5).getMillis.toString
      val filter = new SessionTimeoutFilter(clock, timeoutDuration, Set("whitelisted"), onlyWipeAuthToken = true)
      implicit val rh = exampleRequest.withSession(
        lastRequestTimestamp -> oldTimestamp,
        authToken -> "a-token",
        token -> "another-token",
        userId -> "a-userId",
        "custom" -> "custom",
        "whitelisted" -> "whitelisted")

      val result = filter.apply { req =>
        req.session.get("custom") shouldBe Some("custom")
        req.session.get(authToken) shouldBe None
        req.session.get(userId) shouldBe None
        req.session.get(token) shouldBe None
        Future.successful(Results.Ok)
      }(rh)

      result.futureValue.session.get("custom") shouldBe Some("custom")
      result.futureValue.session.get(authToken) shouldBe None
    }

    "update old timestamp with current time" in {
      implicit val rh = exampleRequest.withSession(lastRequestTimestamp -> now.minusDays(1).getMillis.toString)
      val result = filter.apply(successfulResult)(rh)
      result.futureValue.session.get(lastRequestTimestamp) shouldBe Some(now.getMillis.toString)
    }

    "update recent timestamp with current time" in {
      implicit val rh = exampleRequest.withSession(lastRequestTimestamp -> now.minusSeconds(1).getMillis.toString)
      val result = filter.apply(successfulResult)(rh)
      result.futureValue.session.get(lastRequestTimestamp) shouldBe Some(now.getMillis.toString)
    }

    "treat an invalid timestamp as a missing timestamp" in {
      implicit val rh = exampleRequest.withSession(
        lastRequestTimestamp -> "invalid-format",
        authToken -> "a-token",
        token -> "another-token",
        userId -> "a-userId",
        loginOrigin -> "gg",
        "custom" -> "custom")

      val result = filter(successfulResult)(rh)

      result.futureValue.session.get(authToken) shouldBe Some("a-token")
      result.futureValue.session.get(userId) shouldBe Some("a-userId")
      result.futureValue.session.get(token) shouldBe Some("another-token")
      result.futureValue.session.get(loginOrigin) shouldBe Some("gg")
      result.futureValue.session.get("custom") shouldBe Some("custom")
      result.futureValue.session.get(lastRequestTimestamp) shouldBe Some(now.getMillis.toString)
    }

    "ensure non-session cookies are passed through to the action untouched" in {
      val otherCookie = Cookie("aTestName", "aTestValue")

      val cookieResult = (rh: RequestHeader) => {
        rh.cookies.exists(_ == otherCookie) shouldBe true
        Future.successful(Results.Ok)
      }

      val timestamp = now.minusMinutes(5).getMillis.toString
      implicit val rh = exampleRequest
        .withSession(
          lastRequestTimestamp -> timestamp,
          authToken -> "a-token",
          userId -> "some-userId")
        .withCookies(otherCookie)

      val result = filter(cookieResult)(rh)
      result.futureValue.header.status shouldBe 200
    }

  }

  private def exampleRequest = FakeRequest("POST", "/something", FakeHeaders(), AnyContentAsEmpty)
  private val successfulResult = (rh: RequestHeader) => Future.successful(Results.Ok)

  private def onlyContainWhitelistedKeys(additionalSessionKeysToKeep: Set[String] = Set.empty) = new Matcher[Session] {
    override def apply(session: Session): MatchResult = {
      MatchResult(
        (session.data.keySet -- whitelistedSessionKeys -- additionalSessionKeysToKeep).isEmpty,
        s"""Session keys ${session.data.keySet} did not contain only whitelisted keys: $whitelistedSessionKeys""",
        s"""Session keys ${session.data.keySet} contained only whitelisted keys: $whitelistedSessionKeys"""
      )
    }
  }
}
