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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerTest
import play.api.http.HeaderNames
import play.api.mvc.{Result, _}
import play.api.test._

import scala.concurrent.Future

class DefaultToNoCacheFilterSpec extends WordSpecLike with Matchers with MockitoSugar with ScalaFutures with OneAppPerTest {

  private trait Setup extends Results {
    def action(headers: (String, String)*) = {
      val mockAction = mock[(RequestHeader) => Future[Result]]
      val outgoingResponse = Future.successful(Ok.withHeaders(headers:_*))
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    def getResult(headers: (String, String)*) = {
      DefaultToNoCacheFilter(action(headers:_*))(FakeRequest()).futureValue
    }
  }

  "During result post-processing, the filter" should {

    "add a cache-control header if there isn't one" in new Setup {
      getResult() should be(Ok.withHeaders(CommonHeaders.NoCacheHeader))
    }

    "preserve the cache-control header if there is one" in new Setup {
      getResult(HeaderNames.CACHE_CONTROL -> "max-age=300") should be(Ok.withHeaders(HeaderNames.CACHE_CONTROL -> "max-age=300"))
    }

    "leave any other headers alone adding No Cache" in new Setup {
      val otherHeaders = Seq(
        "header1" -> "value1",
        "header2" -> "value2"
        )
      val expHeaders = otherHeaders :+ CommonHeaders.NoCacheHeader

      getResult(otherHeaders:_*) should be(Ok.withHeaders(expHeaders:_*))
    }

    "preserve all headers" in new Setup {
      val headers = Seq(
        "header1" -> "value1",
        HeaderNames.CACHE_CONTROL -> "max-age:765",
        "header2" -> "value2"
      )

      getResult(headers:_*) should be(Ok.withHeaders(headers:_*))
    }
  }
}
