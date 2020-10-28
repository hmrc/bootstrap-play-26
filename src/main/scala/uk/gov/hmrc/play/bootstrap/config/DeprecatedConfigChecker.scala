/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.config

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}

@Singleton
class DeprecatedConfigChecker @Inject()(
  configuration: Configuration
) {

  private val logger = Logger(getClass)

  val deprecatedKeys = List(
    "httpHeadersWhitelist"     -> "bootstrap.http.headersAllowlist",
    "csrfexceptions.allowlist" -> "bootstrap.csrfexceptions.allowlist"
  )

  val errs = deprecatedKeys.filter { case (d, _) => configuration.has(d) }
  if (errs.nonEmpty) {
    if (configuration.get[Boolean]("bootstrap.configuration.failOnObsoleteKeys"))
      throw configuration.globalError(
        "The following configurations keys were found which are obsolete. Their presence indicate misconfiguration. You must remove them or use the suggested alternatives:\n" +
          errs.map { case (d, k) =>  s"'$d' - Please use '$k' instead" }.mkString("\n")
      )
    else
      errs.foreach { case (d, k) =>
        logger.warn(s"The configuration key '$d' is no longer supported and is being IGNORED! Please use '$k' instead.")
      }
  }
}
