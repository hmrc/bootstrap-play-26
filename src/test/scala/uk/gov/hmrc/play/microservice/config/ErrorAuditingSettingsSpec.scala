///*
// * Copyright 2017 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.play.microservice.config
//
//import org.mockito.ArgumentCaptor
//import org.mockito.Matchers._
//import org.mockito.Mockito._
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.mock.MockitoSugar
//import org.scalatest.{Matchers, WordSpecLike}
//import play.api.GlobalSettings
//import play.api.mvc.{RequestHeader, Result, Results}
//import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, NotFoundException}
//import uk.gov.hmrc.play.audit.EventTypes
//import uk.gov.hmrc.play.audit.http.connector.AuditConnector
//import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
//import uk.gov.hmrc.play.audit.model.DataEvent
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{ExecutionContext, Future}
//
//class ErrorAuditingSettingsSpec extends WordSpecLike with Matchers with ScalaFutures with MockitoSugar {
//
//  trait ParentHandler extends GlobalSettings with Results {
//    var onBadRequestCalled = false
//
//    override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
//      onBadRequestCalled = true
//      Future.successful(BadRequest)
//    }
//
//    var onHandlerNotFoundCalled = false
//
//    override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
//      onHandlerNotFoundCalled = true
//      Future.successful(NotFound)
//    }
//
//    var onErrorCalled = false
//
//    override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
//      onErrorCalled = true
//      Future.successful(InternalServerError)
//    }
//  }
//
//  class TestErrorAuditing(override val auditConnector: AuditConnector) extends ParentHandler with ErrorAuditingSettings {
//    override lazy val appName = "app"
//  }
//
//  "in a case of application error we" should {
//
//    "send ServerInternalError event to DataStream for an Exception that occurred in the microservice" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onError(new DummyRequestHeader(), new Exception("a generic application exception")).futureValue
//
//      val event = verifyAndRetrieveEvent(mockConnector)
//      event.auditType shouldBe EventTypes.ServerInternalError
//    }
//
//    "send ResourceNotFound event to DataStream for a NotFoundException that occurred in the microservice" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onError(new DummyRequestHeader(), new NotFoundException("test")).futureValue
//
//      val event = verifyAndRetrieveEvent(mockConnector)
//      event.auditType shouldBe EventTypes.ResourceNotFound
//    }
//
//    "send ServerValidationError event to DataStream for a JsValidationException that occurred in the microservice" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onError(new DummyRequestHeader(), new JsValidationException("GET", "", classOf[String], "")).futureValue
//
//      val event = verifyAndRetrieveEvent(mockConnector)
//      event.auditType shouldBe EventTypes.ServerValidationError
//    }
//
//    "chain onError call to parent" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onError(new DummyRequestHeader(), new NotFoundException("test")).futureValue
//
//      auditing.onErrorCalled shouldBe true
//    }
//
//  }
//
//  "in a case of the microservice endpoint not being found we" should {
//
//    "send ResourceNotFound event to DataStream" in {
//
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onHandlerNotFound(new DummyRequestHeader()).futureValue
//
//      val event = verifyAndRetrieveEvent(mockConnector)
//      event.auditType shouldBe EventTypes.ResourceNotFound
//    }
//
//    "chain onHandlerNotFound call to parent" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onHandlerNotFound(new DummyRequestHeader()).futureValue
//
//      auditing.onHandlerNotFoundCalled shouldBe true
//    }
//
//  }
//
//  "in a case of incorrect data being sent to the microservice endpoint we" should {
//
//    "send ServerValidationError event to DataStream" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onBadRequest(new DummyRequestHeader(), "error message").futureValue
//
//      val event = verifyAndRetrieveEvent(mockConnector)
//      event.auditType shouldBe EventTypes.ServerValidationError
//    }
//
//    "chain onBadRequest call to parent" in {
//      val mockConnector = createMockConnector
//      val auditing = new TestErrorAuditing(mockConnector)
//
//      auditing.onBadRequest(new DummyRequestHeader(), "error message").futureValue
//
//      auditing.onBadRequestCalled shouldBe true
//    }
//  }
//
//  def createMockConnector: AuditConnector = {
//    val connector = mock[AuditConnector]
//    when(connector.sendEvent(any[DataEvent])).thenReturn(Future {Success})
//    connector
//  }
//
//  def verifyAndRetrieveEvent(connector: AuditConnector): DataEvent = {
//    val captor = ArgumentCaptor.forClass(classOf[DataEvent])
//    verify(connector).sendEvent(captor.capture)(any[HeaderCarrier], any[ExecutionContext])
//    captor.getValue
//  }
//}
