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

package uk.gov.hmrc.play.bootstrap.config

import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}
import uk.gov.hmrc.play.test.UnitSpec

class LoadAuditingConfigSpec extends UnitSpec {

  "LoadAuditingConfig" should {

    "look for config first under <env>.auditing" in {

      val config = Configuration(
        "auditing.enabled" -> "false",
        "Test.auditing.enabled" -> "true",
        "Test.auditing.traceRequests" -> "true",
        "Test.auditing.consumer.baseUri.host" -> "localhost",
        "Test.auditing.consumer.baseUri.port" -> "8100"
      )

      LoadAuditingConfig(config, Environment.simple(), "auditing") shouldBe AuditingConfig(Some(Consumer(BaseUri("localhost", 8100, "http"))), enabled = true)
    }

    "look for config first under auditing if not overridden in <env>.auditing" in {
      val config = Configuration(
        "auditing.enabled" -> "true",
        "auditing.traceRequests" -> "true",
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> "8100"
      )

      LoadAuditingConfig(config, Environment.simple(), "auditing") shouldBe AuditingConfig(Some(Consumer(BaseUri("localhost", 8100, "http"))), enabled = true)
    }

    "allow audit to be disabled" in {
      val config = Configuration(
        "auditing.enabled" -> "false"
      )

      LoadAuditingConfig(config, Environment.simple(), "auditing") shouldBe AuditingConfig(None, enabled = false)
    }


  }

}
