package org.scalatra

import javax.servlet.http.HttpSession
import servlet.ServletSession

/**
 * This trait provides session support for stateful applications.
 */
trait SessionSupport {
  /**
   * The current session.  Creates a session if none exists.
   */
  implicit def session: HttpSession

  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession]

  protected implicit def enrichSession(session: HttpSession): ServletSession = 
    ServletSession(session)
}
