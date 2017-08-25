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

package uk.gov.hmrc.play.bootstrap.filters.microservice

import javax.inject.Inject

import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.stage._
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.http.HttpEntity.Streamed
import play.api.libs.streams.Accumulator
import play.api.mvc.{Result, _}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.EventKeys._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.ControllerConfigs
import uk.gov.hmrc.play.bootstrap.filters.AuditFilter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}


trait MicroserviceAuditFilter extends AuditFilter with HttpAuditEvent {

  def auditConnector: AuditConnector

  def controllerNeedsAuditing(controllerName: String): Boolean

  implicit def mat: Materializer

  val maxBodySize = 32665

  val requestReceived = "RequestReceived"

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val next: Accumulator[ByteString, Result] = nextFilter(requestHeader)
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(requestHeader.headers)

      val loggingContext = s"${requestHeader.method} ${requestHeader.uri}"

      def performAudit(requestBody: String, maybeResult: Try[Result])(responseBody: String): Unit = {
        maybeResult match {
          case Success(result) =>
            auditConnector.sendEvent(
              dataEvent(requestReceived, requestHeader.uri, requestHeader,
                Map(ResponseMessage -> responseBody, StatusCode -> result.header.status.toString)))
          case Failure(f) =>
            auditConnector.sendEvent(
              dataEvent(requestReceived, requestHeader.uri, requestHeader,
                Map(FailedRequestMessage -> f.getMessage)))
        }
      }

      if (needsAuditing(requestHeader)) {
        onCompleteWithInput(loggingContext, next, performAudit)
      } else next
    }
  }

  protected def needsAuditing(request: RequestHeader): Boolean =
    (for (controllerName <- request.tags.get(play.routing.Router.Tags.ROUTE_CONTROLLER))
      yield controllerNeedsAuditing(controllerName)).getOrElse(true)

  protected def onCompleteWithInput(loggingContext: String, next: Accumulator[ByteString, Result], handler: (String, Try[Result]) => String => Unit): Accumulator[ByteString, Result] = {
    val requestBodyPromise = Promise[String]()
    val requestBodyFuture = requestBodyPromise.future

    var requestBody: String = ""
    def callback(body: ByteString): Unit = {
      requestBody = body.decodeString("UTF-8")
      requestBodyPromise success requestBody
    }

    //grabbed from plays csrf filter
    val wrappedAcc: Accumulator[ByteString, Result] = Accumulator(
      Flow[ByteString].via(new RequestBodyCaptor(loggingContext, maxBodySize, callback))
        .splitWhen(_ => false)
        .prefixAndTail(0)
        .map(_._2)
        .concatSubstreams
        .toMat(Sink.head[Source[ByteString, _]])(Keep.right)
    ).mapFuture { bodySource =>
      next.run(bodySource)
    }

    wrappedAcc.mapFuture { result =>
      requestBodyFuture flatMap { res => {
        val auditedBody = result.body match {
          case str: Streamed => {
            val auditFlow = Flow[ByteString].alsoTo(new ResponseBodyCaptor(loggingContext, maxBodySize, handler(requestBody, Success(result))))
            str.copy(data = str.data.via(auditFlow))
          }
          case h: HttpEntity => {
            h.consumeData map { rb =>
              val auditString = if (rb.size > maxBodySize) {
                Logger.warn(s"txm play auditing: $loggingContext response body ${rb.size} exceeds maxLength ${maxBodySize} - do you need to be auditing this payload?")
                rb.take(maxBodySize).decodeString("UTF-8")
              } else {
                rb.decodeString("UTF-8")
              }
              handler(res, Success(result))(auditString)
            }
            h
          }
        }
        Future(result.copy(body = auditedBody))
      }
      }
    }.recover[Result] {
      case ex: Throwable =>
        handler(requestBody, Failure(ex))("")
        throw ex
    }
  }

}


trait HttpAuditEvent {

  import uk.gov.hmrc.play.audit.AuditExtensions._

  def appName: String

  object auditDetailKeys {
    val Input = "input"
    val Method = "method"
    val UserAgentString = "userAgentString"
    val Referrer = "referrer"
  }

  object headers {
    val UserAgent = "User-Agent"
    val Referer = "Referer"
  }

  protected def dataEvent(eventType: String, transactionName: String, request: RequestHeader, detail: Map[String, String] = Map())
                                 (implicit hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)): DataEvent = {

    import auditDetailKeys._
    import headers._
    import uk.gov.hmrc.play.audit.http.HeaderFieldsExtractor._

    val requiredFields = hc.toAuditDetails(Input -> s"Request to ${request.path}",
      Method -> request.method.toUpperCase,
      UserAgentString -> request.headers.get(UserAgent).getOrElse("-"),
      Referrer -> request.headers.get(Referer).getOrElse("-"))

    val tags = hc.toAuditTags(transactionName, request.path)

    DataEvent(appName, eventType, detail = detail ++ requiredFields ++ optionalAuditFieldsSeq(request.headers.toMap), tags = tags)
  }
}


protected[filters] class RequestBodyCaptor(val loggingContext: String, val maxBodyLength: Int, callback: (ByteString) => Unit) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in = Inlet[ByteString]("ReqBodyCaptor.in")
  val out = Outlet[ByteString]("ReqBodyCaptor.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer: ByteString = ByteString.empty
    private var bodyLength = 0

    setHandlers(in, out, new InHandler with OutHandler {

      override def onPull(): Unit = {
        pull(in)
      }

      override def onPush(): Unit = {
        val chunk = grab(in)
        bodyLength += chunk.length
        if (buffer.size < maxBodyLength)
          buffer ++= chunk
        push(out, chunk)
      }

      override def onUpstreamFinish(): Unit = {
        if (bodyLength > maxBodyLength)
          Logger.warn(s"txm play auditing: $loggingContext sanity check request body ${bodyLength} exceeds maxLength ${maxBodyLength} - do you need to be auditing this payload?")
        callback(buffer.take(maxBodyLength))
        if (isAvailable(out) && buffer == ByteString.empty)
          push(out, buffer)
        completeStage()
      }
    })
  }
}


protected[filters] class ResponseBodyCaptor(val loggingContext: String, val maxBodyLength: Int, performAudit: (String) => Unit)
  extends GraphStage[SinkShape[ByteString]] {
  val in = Inlet[ByteString]("RespBodyCaptor.in")
  override val shape = SinkShape.of(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer: ByteString = ByteString.empty
    private var bodyLength = 0

    override def preStart(): Unit = pull(in)

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val chunk = grab(in)
        bodyLength += chunk.length
        if (buffer.size < maxBodyLength)
          buffer ++= chunk
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        if (bodyLength > maxBodyLength)
          Logger.warn(s"txm play auditing: $loggingContext sanity check request body ${bodyLength} exceeds maxLength ${maxBodyLength} - do you need to be auditing this payload?")
        performAudit(buffer.take(maxBodyLength).decodeString("UTF-8"))
        completeStage()
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        performAudit("")
        super.onUpstreamFailure(ex)
      }

    })
  }
}

class DefaultMicroserviceAuditFilter @Inject() (
                                               configuration: Configuration,
                                               controllerConfigs: ControllerConfigs,
                                               override val auditConnector: AuditConnector,
                                               override val mat: Materializer
                                               ) extends MicroserviceAuditFilter {

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    controllerConfigs.get("controllerName").auditing

  override val appName: String = configuration.getString("appName").getOrElse("APP NAME NOT SET")
}
