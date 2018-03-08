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

package uk.gov.hmrc.play.bootstrap.controller

import play.api.mvc._
import play.api.http.MimeTypes

import scala.concurrent.Future
import play.api.mvc.Result
import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.util.{Failure, Success, Try}

trait Utf8MimeTypes {
  self: Controller =>

  override val JSON = s"${MimeTypes.JSON};charset=utf-8"

  override def HTML(implicit codec: Codec) = s"${MimeTypes.HTML};charset=utf-8"
}

trait BaseController extends Controller with Utf8MimeTypes {

  implicit def hc(implicit rh: RequestHeader) = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

  protected def withJsonBody[T](
    f: (T) => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]) =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) =>
        Future.successful(BadRequest(s"Invalid ${m.runtimeClass.getSimpleName} payload: $errs"))
      case Failure(e) => Future.successful(BadRequest(s"could not parse body due to ${e.getMessage}"))
    }

}
