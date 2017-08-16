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
import org.mockito.Mockito._
import org.scalatest.{LoneElement, Matchers, WordSpec}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.http.NotFoundException
import play.api.test.Helpers._

import scala.concurrent.Future

trait MaterializerSupport {
  implicit val system: ActorSystem = ActorSystem("Sys")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
}

class JsonErrorHandlerSpec extends WordSpec with Matchers with ScalaFutures with MockitoSugar with LoneElement with Eventually with MaterializerSupport {

  "error handling in onError function" should {

    "convert a NotFoundException to NotFound response" in new Setup {
      val resultF: Future[Result] = jsh.onServerError(requestHeader, new NotFoundException("test"))
      status(resultF) shouldEqual NOT_FOUND
      contentAsJson(resultF) shouldEqual Json.parse("""{"statusCode":404,"message":"test"}""")
    }

    "convert a BadRequestException to NotFound response" in new Setup {
      val resultF: Future[Result] = jsh.onClientError(requestHeader, 400, "bad request")
      status(resultF) shouldEqual BAD_REQUEST
      contentAsJson(resultF) shouldEqual Json.parse("""{"statusCode":400,"message":"bad request"}""")
    }

    "convert an UnauthorizedException to Unauthorized response" in new Setup {
      val resultF: Future[Result] = jsh.onClientError(requestHeader, 401, "unauthorized")
      status(resultF) shouldEqual UNAUTHORIZED
      contentAsJson(resultF) shouldEqual Json.parse("""{"statusCode":401,"message":"unauthorized"}""")
    }

    "convert an Exception to InternalServerError" in new Setup {
      val resultF: Future[Result] = jsh.onServerError(requestHeader, new Exception("any application exception"))
      status(resultF) shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(resultF) shouldEqual Json.parse("""{"statusCode":500,"message":"any application exception"}""")
    }

    "log one error message for each exception" in new Setup {
      when(requestHeader.method).thenReturn(method)
      when(requestHeader.uri).thenReturn(uri)
    }

    sealed trait Setup {
      val method = "some-method"
      val uri = "some-uri"
      val requestHeader = mock[RequestHeader]
      val jsh = new JsonErrorHandler {}
    }
  }
}
