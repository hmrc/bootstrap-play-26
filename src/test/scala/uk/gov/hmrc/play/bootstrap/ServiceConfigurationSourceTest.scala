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

package uk.gov.hmrc.play.bootstrap

import org.scalatest.{ShouldMatchers, WordSpecLike}
import play.api.Configuration

class ServiceConfigurationSourceTest extends WordSpecLike with ShouldMatchers {

  "Service configuration source should create the configuration object for the service if configuration exists" in {

    val source = new ServiceConfigurationSource(
      Configuration(
        "microservice.services.service1.foo" -> "bar"
      )
    )

    val serviceConfiguration = source.configurationForService("service1")

    serviceConfiguration shouldBe defined
    serviceConfiguration.get.getString("foo") shouldBe Some("bar")

  }

  "Service configuration source should return None if service configuration doesn't exist" in {
    val source = new ServiceConfigurationSource(Configuration())

    val serviceConfiguration = source.configurationForService("service2")

    serviceConfiguration shouldBe None
  }

  "It should be possible to retrieve service URL from the service configuration" in {
    val source = new ServiceConfigurationSource(
      Configuration(
        "microservice.services.service1.host" -> "localhost",
        "microservice.services.service1.port" -> "8080",
        "microservice.services.service1.protocol" -> "http"
      )
    )

    val serviceConfiguration = source.configurationForService("service1")

    serviceConfiguration shouldBe defined
    serviceConfiguration.get.getBaseUrl shouldBe "http://localhost:8080"
  }

  "Trying to retrieve service URL from the service configuration should fail if host is missing" in {
    val source = new ServiceConfigurationSource(
      Configuration(
        "microservice.services.service1.host" -> "localhost",
        "microservice.services.service1.port" -> "8080",
        "microservice.services.service1.protocol" -> "http"
      )
    )

    val serviceConfiguration = source.configurationForService("service1")

    serviceConfiguration shouldBe defined
    serviceConfiguration.get.getBaseUrl shouldBe "http://localhost:8080"
  }

  "Trying to retrieve service URL from the service configuration should fail if port is missing" in {

  }

  "It should be possible to customize default service protocol" in {

  }

  "Protocol specified for the service takes precedence over the default protocol" in {

  }

  "If no protocol specified for the service and no default protocol, fallback to HTTP" in {

  }

}
