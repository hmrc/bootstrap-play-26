package uk.gov.hmrc.play.microservice.filters

import akka.stream.Materializer
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import play.api.LoggerLike
import play.api.mvc.Results
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

import scala.concurrent.{ExecutionContext, Future}

class LoggingFilterSpec extends WordSpecLike with MockitoSugar with Matchers with OptionValues with FutureAwaits with DefaultAwaitTimeout with Eventually {

  class LoggingFilterTest (loggerIn: LoggerLike, controllerNeedsLogging: Boolean)(implicit val mat: Materializer) extends LoggingFilter {
    override def logger = loggerIn
    override def controllerNeedsLogging(controllerName: String): Boolean = controllerNeedsLogging
    override implicit def ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  }

  "the LoggingFilter should" should {

    def buildFakeLogger() = new LoggerLike {
      var lastInfoMessage: Option[String] = None
      override val logger: Logger = NOPLogger.NOP_LOGGER
      override def info(s: => String): Unit = {
        lastInfoMessage = Some(s)
      }
    }

    def requestWith(loggingFilter: LoggingFilter, someTags: Map[String, String] = Map()) = {
      loggingFilter.apply(rh => Future.successful(Results.NoContent))(FakeRequest().copyFakeRequest(tags = someTags))
    }

    "log when a requests' path matches a controller which is configured to log" in {
      val fakeLogger = buildFakeLogger()

      implicit val mat: Materializer = mock[Materializer]
      val loggingFilter = new LoggingFilterTest(fakeLogger, true)

      await(requestWith(loggingFilter))

      eventually {
        fakeLogger.lastInfoMessage.value.length should be > 0
      }
    }

    "not log when a requests' path does not match a controller which is not configured to log" in {
      val fakeLogger = buildFakeLogger()

      implicit val mat: Materializer = mock[Materializer]
      val loggingFilter = new LoggingFilterTest(fakeLogger, false)

      await(requestWith(loggingFilter, Map(play.routing.Router.Tags.ROUTE_CONTROLLER -> "exists")))

      fakeLogger.lastInfoMessage shouldBe None
    }
  }
}