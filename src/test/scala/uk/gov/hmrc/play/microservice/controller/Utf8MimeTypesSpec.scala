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

package uk.gov.hmrc.play.microservice.controller

import org.scalatest.{Matchers, WordSpecLike}
import play.api.mvc.{Codec, Controller}


class Utf8MimeTypesSpec extends WordSpecLike with Matchers {

  implicit val codec = Codec.utf_8

  "Controller minetypes" should {

    "have default application json" in {

      val controller = new Controller {}
      val applicationJsonWithUtf8Charset = controller.JSON

      applicationJsonWithUtf8Charset should not be "application/json;charset=utf-8"
    }

    "have application json with utf8 character set" in {

      val controller = new Controller with Utf8MimeTypes {}
      val applicationJsonWithUtf8Charset = controller.JSON

      applicationJsonWithUtf8Charset shouldBe "application/json;charset=utf-8"
    }

    "have text html with utf8 character set" in {

      val controller = new Controller with Utf8MimeTypes {}
      val textHtmlWithUtf8Charset = controller.HTML

      textHtmlWithUtf8Charset shouldBe "text/html;charset=utf-8"
    }
  }
}
