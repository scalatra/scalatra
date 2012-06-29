package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse}

object MethodOverride {
  val ParamName = "_method"
  val HeaderName = "X-HTTP-METHOD-OVERRIDE"
}
/**
 * Mixin for clients that only support a limited set of HTTP verbs.  If the
 * request is a POST and the `_method` request parameter is set, the value of
 * the `_method` parameter is treated as the request's method.
 */
trait MethodOverride extends Handler {
  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val req2 = req.requestMethod match {
      case Post =>
        new HttpServletRequestWrapper(req) {
	  override def getMethod(): String = 
	    methodOverride(req) getOrElse req.getMethod
	}
      case _ =>
        req
    }
    super.handle(req2, res)
  }

  private def methodOverride(req: HttpServletRequest) = {
    import MethodOverride._
    (req.parameters.get(ParamName) orElse req.headers.get(HeaderName))
  }
}
