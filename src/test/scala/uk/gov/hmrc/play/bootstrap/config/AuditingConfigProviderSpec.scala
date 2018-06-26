/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.config

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.{Configuration, Mode}
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}
import org.mockito.Mockito.when

class AuditingConfigProviderSpec extends WordSpec with Matchers with MockitoSugar {

  private val mockedRunMode = mock[RunMode]
  when(mockedRunMode.env).thenReturn("Test")

  "LoadAuditingConfig" should {

    "use env specific settings if these provided" in {
      val config = Configuration(
        "Test.auditing.enabled"               -> "true",
        "Test.auditing.traceRequests"         -> "true",
        "Test.auditing.consumer.baseUri.host" -> "localhost",
        "Test.auditing.consumer.baseUri.port" -> "8100",
        "auditing.enabled"                    -> "false",
        "auditing.traceRequests"              -> "false",
        "auditing.consumer.baseUri.host"      -> "foo",
        "auditing.consumer.baseUri.port"      -> "1234"
      )

      new AuditingConfigProvider(config, mockedRunMode)
        .get() shouldBe AuditingConfig(Some(Consumer(BaseUri("localhost", 8100, "http"))), enabled = true)
    }

    "fallback to non-env specific config" in {
      val configuration = Configuration(
        "auditing.enabled"               -> "true",
        "auditing.traceRequests"         -> "true",
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> "8100"
      )

      new AuditingConfigProvider(configuration, mockedRunMode)
        .get() shouldBe AuditingConfig(Some(Consumer(BaseUri("localhost", 8100, "http"))), enabled = true)
    }

    "allow audit to be disabled" in {
      val config = Configuration(
        "auditing.enabled" -> "false"
      )

      new AuditingConfigProvider(config, mockedRunMode).get() shouldBe AuditingConfig(None, enabled = false)
    }
  }
}
