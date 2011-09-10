package org.scalatra

import javax.servlet.http.{HttpServletRequestWrapper, HttpServletRequest, HttpServletResponse}

/**
 * Mixin for clients that only support a limited set of HTTP verbs.  If the
 * request is a POST and the `_method` request parameter is set, the value of
 * the `_method` parameter is treated as the request's method.
 */
trait MethodOverride extends Handler {
  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val req2 = req.getMethod match {
      case "POST" =>
        req.getParameter(paramName) match {
          case null => req
          case method => new HttpServletRequestWrapper(req) { override def getMethod = method.toUpperCase }
        }
      case _ =>
        req
    }
    super.handle(req2, res)
  }

  private val paramName = "_method"
}
