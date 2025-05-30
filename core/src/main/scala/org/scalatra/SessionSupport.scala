package org.scalatra

import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpSession}

import org.scalatra.servlet.ServletApiImplicits

/** This trait provides session support for stateful applications.
  */
trait SessionSupport { self: ServletApiImplicits =>

  /** The current session. Creates a session if none exists.
    */
  implicit def session(implicit request: HttpServletRequest): HttpSession = request.getSession

  def session(key: String)(implicit request: HttpServletRequest): Any = session(using request)(key)

  /** The current session. If none exists, None is returned.
    */
  def sessionOption(implicit request: HttpServletRequest): Option[HttpSession] = Option(request.getSession(false))

}
