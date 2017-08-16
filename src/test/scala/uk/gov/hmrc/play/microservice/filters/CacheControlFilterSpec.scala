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

package uk.gov.hmrc.play.microservice.filters

import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerTest
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, _}
import play.api.test._

import scala.concurrent.Future

class CacheControlFilterSpec extends WordSpecLike with Matchers with MockitoSugar with ScalaFutures with OneAppPerTest {

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      "caching.allowedContentTypes.0" -> "image/",
      "caching.allowedContentTypes.1" -> "text/css",
      "caching.allowedContentTypes.2" -> "application/javascript"
    ).build()

  private trait Setup extends Results {

    val expectedCacheControlHeader = HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0"

    val resultFromAction: Result = Ok

    val cacheControlFilter = new CacheControlFilter(application.configuration)

    lazy val action = {
      val mockAction = mock[(RequestHeader) => Future[Result]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    def requestPassedToAction = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue
    }
  }

  "During request pre-processing, the filter" should {

    "do nothing, just pass on the request" in new Setup {
      cacheControlFilter(action)(FakeRequest())
      requestPassedToAction should ===(FakeRequest())
    }
  }

  "During result post-processing, the filter" should {

    "add a cache-control header if there isn't one and the response has no content type" in new Setup {
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction.withHeaders(expectedCacheControlHeader))
    }

    "add a cache-control header if there isn't one and the response does not have an excluded content type" in new Setup {
      override val resultFromAction: Result = Ok.as("text/html")
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction.withHeaders(expectedCacheControlHeader))
    }

    "not add a cache-control header if there isn't one but the response is an exact match for an excluded content type" in new Setup {
      override val resultFromAction: Result = Ok.as("text/css")
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "not add a cache-control header if there isn't one but the response is an exact match for an mime part of an excluded content type" in new Setup {
      override val resultFromAction: Result = Ok.as("text/css; charset=utf-8")
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "not add a cache-control header if there isn't one but the response is an exact match for an category of the mime part of an excluded content type" in new Setup {
      override val resultFromAction: Result = Ok.as("image/png")
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "not add a cache-control header if there is no content type but the status is NOT MODIFIED" in new Setup {
      override val resultFromAction: Result = NotModified
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "replace any existing cache-control header" in new Setup {
      override val resultFromAction = Ok.withHeaders(HeaderNames.CACHE_CONTROL -> "someOtherValue")
      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction.withHeaders(expectedCacheControlHeader))
    }

    "leave any other headers alone" in new Setup {
      override val resultFromAction = Ok.withHeaders(
        "header1" -> "value1",
        HeaderNames.CACHE_CONTROL -> "someOtherValue",
        "header2" -> "value2")

      cacheControlFilter(action)(FakeRequest()).futureValue should be(resultFromAction.withHeaders(expectedCacheControlHeader))
    }
  }

//  "Creating the filter from config" should {
//    "load the correct values" in new WithApplication(new GuiceApplicationBuilder().configure("caching" -> List("image/", "text/")).build()) {
//      CacheControlFilter.fromConfig("caching").cachableContentTypes should be(List("image/", "text/"))
//    }
//  }
}
