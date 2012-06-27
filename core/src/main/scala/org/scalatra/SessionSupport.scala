package org.scalatra

/**
 * This trait provides abstract session support for stateful applications.
 * The session may be clientside or serverside.
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
}
