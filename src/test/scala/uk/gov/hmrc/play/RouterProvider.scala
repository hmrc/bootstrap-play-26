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

package uk.gov.hmrc.play

import javax.inject.{Inject, Provider}
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.Router.Routes

import scala.language.implicitConversions

/**
  * Allows providing routes definition inline without using deprecated play.api.mvc.Action
  *
  * for example:
  * {{{
  *
  *  import play.api.routing.sird._
  *  import play.api.inject.bind
  *
  *  val builder: GuiceApplicationBuilder =
  *    new GuiceApplicationBuilder()
  *      .overrides(
  *        bind[Router].toProvider[RouterProvider],
  *        bind[RoutesDefinition].toInstance(Action => {
  *          case GET(p"/test") =>
  *            Action { _ =>
  *              Ok
  *            }
  *        })
  *      )
  *
  * }}}
  */
class RouterProvider @Inject()(actionBuilder: DefaultActionBuilder, routes: RoutesDefinition) extends Provider[Router] {
  override def get: Router = Router.from(routes(actionBuilder))
}

trait RoutesDefinition extends (DefaultActionBuilder => Routes)

object RoutesDefinition {
  implicit def asRoutesDefinition(f: DefaultActionBuilder => Routes): RoutesDefinition =
    new RoutesDefinition {
      def apply(actionBuilder: DefaultActionBuilder): Routes = f(actionBuilder)
    }
}
