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

package uk.gov.hmrc.play.bootstrap.filters.microservice

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.filters.FilterFlowMock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MicroserviceAuditFilterSpec
    extends WordSpec
    with Matchers
    with Eventually
    with ScalaFutures
    with FilterFlowMock
    with MockitoSugar {

  "AuditFilter" should {
    val applicationName = "app-name"

    val requestReceived  = "RequestReceived"
    val xRequestId       = "A_REQUEST_ID"
    val xSessionId       = "A_SESSION_ID"
    val deviceID         = "A_DEVICE_ID"
    val akamaiReputation = "AN_AKAMAI_REPUTATION"

    implicit val patienceConfig: PatienceConfig =
      PatienceConfig(Span(5, Seconds), Span(15, Millis))

    implicit val system       = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val hc           = HeaderCarrier
    val request = FakeRequest().withHeaders(
      "X-Request-ID"      -> xRequestId,
      "X-Session-ID"      -> xSessionId,
      "deviceID"          -> deviceID,
      "Akamai-Reputation" -> akamaiReputation)

    def createAuditFilter(connector: AuditConnector) =
      new MicroserviceAuditFilter {
        override val auditConnector: AuditConnector = connector
        override val appName: String                = applicationName

        override def controllerNeedsAuditing(controllerName: String): Boolean = true

        implicit val system                     = ActorSystem("test")
        implicit override def mat: Materializer = ActorMaterializer()
      }

    "audit a request and response with header information" in {
      val mockAuditConnector = mock[AuditConnector]
      val auditFilter        = createAuditFilter(mockAuditConnector)

      when(mockAuditConnector.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future {
        Success
      })

      val result = await(auditFilter.apply(nextAction)(request).run)

      await(result.body.dataStream.runForeach({ i =>
        }))

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockAuditConnector, times(1)).sendEvent(captor.capture)(any[HeaderCarrier], any[ExecutionContext])
        verifyNoMoreInteractions(mockAuditConnector)
        val event = captor.getValue

        event.auditSource               shouldBe applicationName
        event.auditType                 shouldBe requestReceived
        event.tags("X-Request-ID")      shouldBe xRequestId
        event.tags("X-Session-ID")      shouldBe xSessionId
        event.tags("Akamai-Reputation") shouldBe akamaiReputation
        event.detail("deviceID")        shouldBe deviceID
        event.detail("responseMessage") shouldBe actionNotFoundMessage
      }
    }

    "audit a response even when an action further down the chain throws an exception" in {
      val mockAuditConnector = mock[AuditConnector]
      val auditFilter        = createAuditFilter(mockAuditConnector)

      when(mockAuditConnector.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future {
        Success
      })

      a[RuntimeException] should be thrownBy await(auditFilter.apply(exceptionThrowingAction)(request).run)

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockAuditConnector, times(1)).sendEvent(captor.capture)(any[HeaderCarrier], any[ExecutionContext])
        verifyNoMoreInteractions(mockAuditConnector)
        val event = captor.getValue

        event.auditSource               shouldBe applicationName
        event.auditType                 shouldBe requestReceived
        event.tags("X-Request-ID")      shouldBe xRequestId
        event.tags("X-Session-ID")      shouldBe xSessionId
        event.tags("Akamai-Reputation") shouldBe akamaiReputation
        event.detail("deviceID")        shouldBe deviceID
      }
    }
  }
}
