package controllers

import play.api.http.Status._
import play.api.libs.json._

import scala.xml.Elem

trait HttpGenericException[A <: ExceptionCode] extends Throwable {

  val code: A

  def formatAndLog(
    microService: String,
    requestId: String,
    requestHeaders: Map[String, String],
    requestUri: String)(log: String => Unit): (Int, JsObject) = {
    val js: JsObject = Json.obj(
      "status" -> code.codeHttp,
      "code" -> s"${code.fullCode(microService)}",
      "requestId" -> requestId,
      "headers" -> Json.toJson(requestHeaders))
    log((js ++ Json.obj("message" -> code.message.getOrElse[JsValue](JsString(code.httpMessage)), "uri" -> requestUri)).toString)
    (code.codeHttp, js + ("message", JsString(s"${code.httpMessage}")))
  }

  def formatAndLogXml(microService: String, requestId: String, requestHeaders: Map[String, String], requestUri: String)(log: String => Unit): (Int, Elem) = {
    def headersToXml(name: String, value: String) = <header name={ name }>{ value }</header>
    val xml =
      <response>
        <status>{ code.codeHttp }</status>
        <code>{ code.fullCode(microService) }</code>
        <requestId>{ requestId }</requestId>
        <headers>
          {
            requestHeaders.map {
              case (k, v)=> headersToXml(k, v)
            }
          }
        </headers>
        <message>{ code.httpMessage }</message>
        <uri>{ requestUri }</uri>
      </response>

    log(xml.toString())
    (code.codeHttp, xml)
  }
}

trait ExceptionCode {
  val service: Option[String]
  val codeError: Int
  val httpMessage: String
  val message: Option[JsValue]
  val codeHttp: Int
  lazy val fullCode = { microService: String =>
    val s = service.getOrElse(microService)
    s"$s-$codeHttp-$codeError"
  }

  /**
   * le format pour les log
   *
   */
  def logMessage = {
    val code = this.getClass.getName.replaceAll("\\$", "").replace("minus", "-").replace("fr.canal.lib.utils.exception.", "")
    s"$code - $httpMessage"
  }

}

object ExceptionCode {
  trait BadRequestException extends ExceptionCode {
    val codeHttp = BAD_REQUEST
  }

  trait NotFoundException extends ExceptionCode {
    val codeHttp = NOT_FOUND
  }

  trait UnauthorizedException extends ExceptionCode {
    val codeHttp = UNAUTHORIZED
  }

  trait ForbiddenException extends ExceptionCode {
    val codeHttp = FORBIDDEN
  }

  trait GoneException extends ExceptionCode {
    val codeHttp = GONE
  }

  trait InternalServerErrorException extends ExceptionCode {
    val codeHttp = INTERNAL_SERVER_ERROR
  }

  trait GatewayTimeout extends ExceptionCode {
    val codeHttp = GATEWAY_TIMEOUT
  }



  trait UnavailableForLegalReasonsException extends ExceptionCode {
    val codeHttp = 451
  }

  object Pass {
    object  badScheme extends ExceptionCode with UnauthorizedException {
      override val service: Option[String] = Some("PASS")
      override val codeError: Int = 3
      override val httpMessage: String = "error authorized"
      override val message: Option[JsValue] = None
    }
  }

  object Tech {
    abstract class TechBase(override val httpMessage: String, override val codeError: Int) extends ExceptionCode {
      val service = Some("TEC")
    }
    case class `500-1`(cause: String, message: Option[JsValue] = None)
      extends TechBase(s"Technical error not managed - [$cause]", 1) with InternalServerErrorException
    case class `500-2`(cause: String, message: Option[JsValue] = None)
      extends TechBase(s"Error during access to data layer - [$cause]", 2) with InternalServerErrorException
  }

}

class HttpRuntimeException[A <: ExceptionCode](override val code: A, cause: Option[Throwable] = None)
  extends RuntimeException(code.logMessage, cause.orNull) with HttpGenericException[A]