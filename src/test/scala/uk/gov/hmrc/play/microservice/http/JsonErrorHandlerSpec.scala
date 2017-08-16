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

package uk.gov.hmrc.play.microservice.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ch.qos.logback.classic.Level
import org.mockito.Mockito._
import org.scalatest.LoneElement
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.test.{LogCapturing, UnitSpec}


trait MaterializerSupport {
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()
}

class JsonErrorHandlerSpec extends UnitSpec with ScalaFutures with MockitoSugar with LogCapturing with LoneElement with Eventually with MaterializerSupport {

  "error handling in onError function" should {

    "convert a NotFoundException to NotFound response" in new Setup {
      val resultF = jsh.onServerError(requestHeader, new NotFoundException("test")).futureValue
      resultF.header.status shouldBe 404
      jsonBodyOf(resultF) shouldBe Json.parse("""{"statusCode":404,"message":"test"}""")
    }

    "convert a BadRequestException to NotFound response" in new Setup {
      val resultF = jsh.onClientError(requestHeader, 400, "bad request").futureValue
      resultF.header.status shouldBe 400
      jsonBodyOf(resultF) shouldBe Json.parse("""{"statusCode":400,"message":"bad request"}""")
    }

    "convert an UnauthorizedException to Unauthorized response" in new Setup {
      val resultF = jsh.onClientError(requestHeader, 401, "unauthorized").futureValue
      resultF.header.status shouldBe 401
      jsonBodyOf(resultF) shouldBe Json.parse("""{"statusCode":401,"message":"unauthorized"}""")
    }

    "convert an Exception to InternalServerError" in new Setup {
      val resultF = jsh.onServerError(requestHeader, new Exception("any application exception")).futureValue
      resultF.header.status shouldBe 500
      jsonBodyOf(resultF) shouldBe Json.parse("""{"statusCode":500,"message":"any application exception"}""")
    }

    "log one error message for each exception" in new Setup {
      when(requestHeader.method).thenReturn(method)
      when(requestHeader.uri).thenReturn(uri)

      withCaptureOfLoggingFrom(Logger) { logEvents =>
        jsh.onServerError(requestHeader, new Exception("any application exception")).futureValue

        verify(requestHeader).method
        verify(requestHeader).uri
        verifyNoMoreInteractions(requestHeader)

        eventually {
          val event = logEvents.loneElement
          event.getLevel shouldBe Level.ERROR
          event.getMessage shouldBe s"! Internal server error, for ($method) [$uri] -> "
        }
      }
    }

    sealed trait Setup {
      val method = "some-method"
      val uri = "some-uri"
      val requestHeader = mock[RequestHeader]
      val jsh = new JsonErrorHandler {}
    }
  }
}
