package uk.gov.hmrc.play.bootstrap.filters.frontend

import javax.inject.Inject

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.{DefaultHttpFilters, HttpFilters}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsString, Json}
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderNames

object HeadersFilterSpec {

  class Filters @Inject() (headersFilter: HeadersFilter) extends DefaultHttpFilters(headersFilter)
}

class HeadersFilterSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  import HeadersFilterSpec._

  val router: Router = {

    import play.api.routing.sird._

    Router.from {
      case GET(p"/test") => Action {
        request =>

          val headers = request.headers.toMap
            .filterKeys(Seq(HeaderNames.xRequestId, HeaderNames.xRequestTimestamp).contains)

          Results.Ok(Json.obj(
            HeaderNames.xRequestId        -> headers.get(HeaderNames.xRequestId),
            HeaderNames.xRequestTimestamp -> headers.get(HeaderNames.xRequestTimestamp)
          ))
      }
    }
  }

  override lazy val app: Application = {

    import play.api.inject._

    new GuiceApplicationBuilder()
      .router(router)
      .overrides(
        bind[HttpFilters].to[Filters]
      )
      .build()
  }

  ".apply" must {

    "add headers to a request which doesn't already have an xRequestId header" in {
      val Some(result) = route(app, FakeRequest(GET, "/test"))
      val body = contentAsJson(result)

      (body \ HeaderNames.xRequestId).toOption mustBe defined
      (body \ HeaderNames.xRequestTimestamp).toOption mustBe defined
    }

    "not add headers to a request which already has an xRequestId header" in {
      val Some(result) = route(app, FakeRequest(GET, "/test").withSession(HeaderNames.xRequestId -> "foo"))
      val body = contentAsJson(result)

      (body \ HeaderNames.xRequestId).get mustEqual JsNull
      (body \ HeaderNames.xRequestTimestamp).get mustEqual JsNull
    }
  }
}
