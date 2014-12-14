package org.scalatra

import javax.servlet.http.{ HttpServletRequest, HttpSession }
import servlet.ServletApiImplicits

/**
 * This trait provides session support for stateful applications.
 */
trait SessionSupport { self: ServletApiImplicits =>
  /**
   * The current session.  Creates a session if none exists.
   */
  implicit def session(implicit request: HttpServletRequest): HttpSession = request.getSession
  def session(key: String)(implicit request: HttpServletRequest): Any = session(request)(key)
  def session(key: Symbol)(implicit request: HttpServletRequest): Any = session(request)(key)

  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption(implicit request: HttpServletRequest): Option[HttpSession] = Option(request.getSession(false))
}
