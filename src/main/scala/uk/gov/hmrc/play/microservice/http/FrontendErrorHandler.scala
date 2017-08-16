package uk.gov.hmrc.play.microservice.http

import akka.stream.Materializer
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.http.HttpErrorHandler
import play.api.i18n.Messages
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.api.{DefaultGlobal, Logger, PlayException}
import play.twirl.api.Html

import scala.concurrent.Future
import scala.util.control.NonFatal

abstract class FrontendErrorHandler()(implicit messages: Messages) extends HttpErrorHandler {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    statusCode match{
      case play.mvc.Http.Status.BAD_REQUEST => Future.successful(BadRequest(badRequestTemplate(request)))
      case play.mvc.Http.Status.NOT_FOUND => Future.successful(NotFound(notFoundTemplate(request)))
      case _ =>
        // This is copied from GlobalSettingsHttpErrorHandler for backward compatibility
        Future.successful(Results.Status(statusCode)(views.html.defaultpages.badRequest(request.method, request.uri, message)))
    }
  }


  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logError(request, exception)
    Future.successful(resolveError(request, exception))
  }


  private implicit def rhToRequest(rh: RequestHeader) : Request[_] = Request(rh, "")

  def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html

  def badRequestTemplate(implicit request: Request[_]): Html = standardErrorTemplate(
    Messages("global.error.badRequest400.title"),
    Messages("global.error.badRequest400.heading"),
    Messages("global.error.badRequest400.message"))

  def notFoundTemplate(implicit request: Request[_]): Html = standardErrorTemplate(
    Messages("global.error.pageNotFound404.title"),
    Messages("global.error.pageNotFound404.heading"),
    Messages("global.error.pageNotFound404.message"))

  def internalServerErrorTemplate(implicit request: Request[_]): Html = standardErrorTemplate(
    Messages("global.error.InternalServerError500.title"),
    Messages("global.error.InternalServerError500.heading"),
    Messages("global.error.InternalServerError500.message"))

  private def logError(request: RequestHeader, ex: Throwable): Unit = {
    try {
      Logger.error(
        """
          |
          |! %sInternal server error, for (%s) [%s] ->
          | """.stripMargin.format(ex match {
          case p: PlayException => "@" + p.id + " - "
          case _ => ""
        }, request.method, request.uri),
        ex
      )

    } catch {
      case NonFatal(e) => DefaultGlobal.onError(request, e)
    }
  }

  def resolveError(rh: RequestHeader, ex: Throwable) = ex match {
    case ApplicationException(domain, result, _) => result
    case _ => InternalServerError(internalServerErrorTemplate(rh)).withHeaders(CACHE_CONTROL -> "no-cache")
  }
}

case class ApplicationException(domain: String, result: Result, message: String) extends Exception(message)
