package org.scalatra

/**
 * This trait provides abstract session support for stateful applications.
 * The session may be clientside or serverside.
 */
trait SessionSupport {

  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession]

  private var _session: Option[HttpSession] = None
  implicit def session: HttpSession = _session getOrElse SessionsDisableException()

  private[scalatra] def session_=(session: HttpSession) = {
    require(session != null, "The session can't be null")
    _session = Some(session)
    session
  }
}
