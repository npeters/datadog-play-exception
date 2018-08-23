package controllers

import javax.inject.Inject
import play.api.mvc._


class TokenValidationException(code: ExceptionCode, cause: Option[Throwable] = None) extends HttpRuntimeException(code, None)

class ApplicationController @Inject()() extends InjectedController {
  protected lazy val log = play.api.Logger("ApplicationController")

  def ok() = Action {
    Ok("ok\n")
  }


  def ko()=  Action {
    if(true){
      throw new TokenValidationException(ExceptionCode.Pass.badScheme)
    }

    Ok("ok\n")

  }




}
