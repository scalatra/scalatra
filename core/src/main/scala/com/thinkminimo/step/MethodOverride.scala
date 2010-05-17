package com.thinkminimo.step

import javax.servlet.http.{HttpServletRequestWrapper, HttpServletRequest, HttpServletResponse}

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