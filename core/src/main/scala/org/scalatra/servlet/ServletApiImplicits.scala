package org.scalatra
package servlet

import java.util.EnumSet
import javax.servlet.DispatcherType
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpSession}

/**
 * Some implicits to make the Servlet API more Scala-idiomatic.
 */
trait ServletApiImplicits {
  implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  implicit def sessionWrapper(s: HttpSession) = new RichSession(s)
  implicit def servletContextWrapper(sc: ServletContext) = new RichServletContext(sc)
  implicit def DefaultDispatchers: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}
