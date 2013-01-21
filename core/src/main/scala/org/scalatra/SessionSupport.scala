package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpSession}
import servlet.ServletApiImplicits

/**
 * This trait provides session support for stateful applications.
 */
trait SessionSupport extends RequestResponse with ServletApiImplicits {
  /**
   * The current session.  Creates a session if none exists.
   */
  implicit def session: HttpSession = request.getSession

  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession] = Option(request.getSession(false))
}
