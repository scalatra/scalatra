package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpSession}
import servlet.ServletSession

/**
 * This trait provides session support for stateful applications.
 */
trait SessionSupport {
  def request: HttpServletRequest

  /**
   * The current session.  Creates a session if none exists.
   */
  implicit def session: HttpSession = request.getSession

  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession] = Option(request.getSession(false))

  protected implicit def enrichSession(session: HttpSession): ServletSession = 
    ServletSession(session)
}
