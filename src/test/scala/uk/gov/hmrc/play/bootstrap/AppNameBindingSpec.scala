/*
 * Copyright 2019 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import play.api.inject.BindingKey
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.bootstrap.binding.AppName

class AppNameBindingSpec extends WordSpec with Matchers  {

  def anApplicationWithBindedName(configFile: String): Unit = {
    val app = new GuiceApplicationBuilder()
      .configure(Configuration(ConfigFactory.load(configFile)))
      .build()

    val injector = app.injector

    "bind application name by Named" in {
      injector.instanceOf(BindingKey(classOf[String]).qualifiedWith("appName")) shouldEqual "test-application"
    }

    "bind application name by binding annotation" in {
      injector.instanceOf(BindingKey(classOf[String]).qualifiedWith[AppName]) shouldEqual "test-application"
    }
  }

  "A microservice" must {
    behave like anApplicationWithBindedName("microservice.test.conf")
  }


  "A frontend" must {
    behave like anApplicationWithBindedName("frontend.test.conf")
  }
}
