package controllers

import akka.stream.Materializer
import play.api._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Filter, RequestHeader}
import play.api.mvc.Result
import play.api.mvc.Results.Status

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.matching.Regex

object ErrorHandlerFilter {
  val log = Logger(s"errorHandler")
  val headerPattern: Regex = "(^XX-.*|^Authorization)".r

  def filterHeader(requestHeader: RequestHeader): Map[String, String] = {
    requestHeader.headers.toSimpleMap.filter {
      case (key, _) => headerPattern.findFirstIn(key).isDefined
    }
  }

  def requestId(requestHeader: RequestHeader): String = requestHeader.attrs.get(Attrs.requestId).getOrElse("no-request-id")

  def errorHandler[A <: ExceptionCode](serviceName: String, requestHeader: RequestHeader, e: HttpGenericException[A]): Result = {

    val requestIdStr = requestId(requestHeader)
    val headers = filterHeader(requestHeader)
    val uri = requestHeader.uri

    def logLevel[A <: ExceptionCode](msg: String): Unit = if (e.code.codeHttp == play.api.http.Status.INTERNAL_SERVER_ERROR) log.error(msg, e)
    else log.debug(msg, e)

    if (requestHeader.headers.get(HeaderNames.ACCEPT).contains(MimeTypes.XML)) {
      val (code, msg) = e.formatAndLogXml(serviceName, requestIdStr, headers, uri)(logLevel)
      Status(code)(msg).as("application/xml; charset=utf-8")
    } else {
      val (code, msg) = e.formatAndLog(serviceName, requestIdStr, headers, uri)(logLevel)
      Status(code)(msg)
    }
  }
}

case class ErrorHandlerFilter(serviceName: String)(implicit val mat: Materializer, val executionContext: ExecutionContext) extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).recover {
      case e: HttpGenericException[_] =>
        ErrorHandlerFilter.errorHandler(serviceName, requestHeader, e)
      case NonFatal(ex) =>
        val e = new HttpRuntimeException(ExceptionCode.Tech.`500-1`(ex.getMessage), Some(ex))
        ErrorHandlerFilter.errorHandler(serviceName, requestHeader, e)
    }
  }
}

object Attrs {
  val REQUEST_ID_HEADER = "XX-Request-Id"
  val requestId: TypedKey[String] = TypedKey(REQUEST_ID_HEADER)
}