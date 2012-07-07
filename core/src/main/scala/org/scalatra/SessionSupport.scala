package org.scalatra

import java.util.concurrent.atomic.AtomicReference

object SessionSupport {
  val SessionKey = getClass.getName
}

/**
 * This trait provides session support for stateful applications.
 */
trait SessionSupport { self: ScalatraApp =>

  import SessionSupport._
  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession] = request.get(SessionKey).map(_.asInstanceOf[HttpSession])

  implicit def session: HttpSession = {
    sessionOption getOrElse SessionsDisableException()
  }
//
//  private[scalatra] def session_=(newSession: HttpSession) = {
//    require(session != null, "The session can't be null")
//    request(SessionKey) = newSession
//    newSession
//  }
}
