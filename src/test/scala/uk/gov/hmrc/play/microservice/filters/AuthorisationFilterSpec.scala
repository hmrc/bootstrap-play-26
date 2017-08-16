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

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.Results._
import play.api.routing.Router.Tags
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{Predicate, RawJsonPredicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.filter.FilterConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class AuthorisationFilterSpec extends WordSpec with ScalaFutures {


  trait ConnectorSuccess {
    val connectorSuccess = true
  }

  trait ConnectorFailure {
    val connectorSuccess = false
  }

  trait Authorised {
    val expectedStatus = Status.OK
  }

  trait Unauthorised {
    val expectedStatus = Status.UNAUTHORIZED
  }

  private trait Setup extends ConfigSetup {

    implicit lazy val hc = HeaderCarrier()

    def connectorSuccess: Boolean

    var predicateJson: JsValue = JsNull

    val filter = new AuthorisationFilter {

      val config = FilterConfig(ConfigFactory.parseString(fullConfig).getConfig("controllers"))

      val connector: AuthConnector = new AuthConnector {
        def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
          predicate match {
            case RawJsonPredicate(value) => predicateJson = value
            case _ => ()
          }
          if (connectorSuccess) Future.successful(retrieval.reads.reads(Json.obj()).get) // always EmptyRetrieval
          else Future.failed(new InsufficientEnrolments)
        }
      }

      override implicit def mat: Materializer = ???
    }

    def path: String

    def expectedStatus: Int

    def controllerName: String = "foo.FooController"

    def authoriseAndValidate(): Unit = {
      val request = FakeRequest("GET", path).withTag(Tags.RouteController, controllerName)

      val resultF = filter(_ => Future.successful(Ok("")))(request)

      whenReady(resultF) { result =>
        result.header.status shouldBe expectedStatus
      }
    }
  }

  "AuthorisationFilter" should {

    "return 200 if the controller is not protected" in new Setup with ConnectorFailure with Authorised {

      override val controllerName = "something.not.InConfig"

      val path = "/foo/enrol1/12345"

      authoriseAndValidate()
    }

    "return 401 if the controller is protected, but the path does not match any pattern" in new Setup with ConnectorSuccess with Unauthorised {

      val path = "/foo/zzz/12345"

      authoriseAndValidate()
    }

    "return 200 if the path matches a pattern and the connector returns 200" in new Setup with ConnectorSuccess with Authorised {

      val path = "/foo/enrol1/12345"

      authoriseAndValidate()
    }

    "return 401 if the path matches a pattern and the connector returns 401" in new Setup with ConnectorFailure with Unauthorised {

      val path = "/foo/enrol1/12345"

      authoriseAndValidate()
    }

    "populate placeholders in the JSON body with path variables" in new Setup with ConnectorSuccess with Authorised {

      val path = "/foo/enrol1/12345"

      authoriseAndValidate()

      Json.stringify(predicateJson) shouldBe """[{"enrolment":"ENROL-1","identifiers":[{"key":"BOO","value":"12345"}]}]"""
    }



  }

}

trait ConfigSetup {


  val enrol1EnrolmentConfig =
    """[{
      |  enrolment = "ENROL-1"
      |  identifiers = [{ key = "BOO", value = "$taxId" }]
      |}]""".stripMargin

  val enrol2EnrolmentConfig =
    """[{
      |  enrolment = "ENROL-2"
      |  identifiers = [{ key = "AHH", value = "$taxId" }]
      |}]""".stripMargin

  val fullConfig =
    s"""
       |controllers {
       |
     |  authorisation = {
       |
     |    enrol1 = {
       |      patterns = [
       |        "/foo/enrol1/:taxId"
       |        "/foo/enrol1/:taxId/:rest"
       |      ],
       |      predicates = $enrol1EnrolmentConfig
       |    }
       |
     |    enrol2 = {
       |      patterns = [
       |        "/foo/enrol2/:taxId"
       |        "/foo/enrol2/:taxId/:rest"
       |      ],
       |      predicates = $enrol2EnrolmentConfig
       |    }
       |
     |  }
       |
     |  foo.FooController = {
       |    authorisedBy = ["enrol1", "enrol2"]
       |    needsLogging = false
       |    needsAuditing = false
       |  }
       |
     |  bar.BarController = {
       |    authorisedBy = ["enrol1"]
       |    needsLogging = false
       |    needsAuditing = false
       |  }
       |
     |  baz.BazController = {
       |    needsLogging = false
       |    needsAuditing = false
       |  }
       |
     |  bim.BimController = {
       |    authorisedBy = ["unknown"]
       |    needsLogging = false
       |    needsAuditing = false
       |  }
       |
     |}""".stripMargin


}
