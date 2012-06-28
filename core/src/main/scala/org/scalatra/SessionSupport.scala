package org.scalatra

/**
 * This trait provides abstract session support for stateful applications.
 * The session may be clientside or serverside.
 */
trait SessionSupport {

  private[scalatra] var _session: Option[HttpSession] = None

  private[scalatra] var _created: Boolean = false

  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession] = if (_created) _session else None

  implicit def session: HttpSession = {
    _created = true
    _session getOrElse SessionsDisableException()
  }

  private[scalatra] def session_=(session: HttpSession) = {
    require(session != null, "The session can't be null")
    _session = Option(session)
    session
  }
}
