package org.scalatra
package servlet

import java.io.{OutputStream, PrintWriter}
import java.net.URI
import java.util.EnumSet
import javax.servlet.DispatcherType
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession => ServletSession}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.collection.JavaConversions._

/**
 * Some implicits to make the Servlet API more Scala-idiomatic.
 */
trait ServletApiImplicits {
  implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  implicit def responseWrapper(r: HttpServletResponse) = RichResponse(r)
  implicit def sessionWrapper(s: ServletSession) = new RichSession(s)
  implicit def servletContextWrapper(sc: ServletContext) = new RichServletContext(sc)
  implicit def DefaultDispatchers: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}
