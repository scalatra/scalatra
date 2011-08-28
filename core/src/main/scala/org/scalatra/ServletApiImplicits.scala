package org.scalatra

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpSession}

/**
 * Some implicits to make the Servlet API more Scala-idiomatic.
 */
trait ServletApiImplicits {
  implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  implicit def sessionWrapper(s: HttpSession) = new RichSession(s)
  implicit def servletContextWrapper(sc: ServletContext) = new RichServletContext(sc)
}

object ServletApiImplicits extends ServletApiImplicits
