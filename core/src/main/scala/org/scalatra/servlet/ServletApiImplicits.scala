package org.scalatra
package servlet

import util.RicherString._

import java.io.{OutputStream, PrintWriter}
import java.net.URI
import java.util.EnumSet
import javax.servlet.DispatcherType
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession => ServletSession, Cookie => ServletCookie}
import scala.collection.JavaConversions._

/**
 * Some implicits to make the Servlet API more Scala-idiomatic.
 */
trait ServletApiImplicits {
  implicit def sessionWrapper(s: ServletSession) = new RichSession(s)
  implicit def DefaultDispatchers: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)

  implicit def cookie2ServletCookie(cookie: Cookie): ServletCookie = {
    import cookie._

    val sCookie = new ServletCookie(name, value)
    if (options.domain.isNonBlank) sCookie.setDomain(options.domain)
    if(options.path.isNonBlank) sCookie.setPath(options.path)
    sCookie.setMaxAge(options.maxAge)
    if(options.secure) sCookie.setSecure(options.secure)
    if(options.comment.isNonBlank) sCookie.setComment(options.comment)
    sCookie.setHttpOnly(options.httpOnly)
    sCookie
  }
}
