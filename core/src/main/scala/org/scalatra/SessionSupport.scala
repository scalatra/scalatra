package org.scalatra

object SessionSupport {
  val SessionKey = getClass.getName
}

/**
 * This trait provides session support for stateful applications.
 */
trait SessionSupport { self: ScalatraApp =>

  import SessionSupport._

  private def sessionFromCookieOrRequest: Option[HttpSession] =
    request.get(SessionKey).map(_.asInstanceOf[HttpSession]) orElse {
      request.cookies get appContext.sessionIdKey flatMap appContext.sessions.get
    }
  /**
   * The current session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession] = sessionFromCookieOrRequest

  implicit def session: HttpSession = {
    appContext.sessions match {
      case _: NoopSessionStore => SessionsDisableException()
      case _ =>
        val current = sessionFromCookieOrRequest
        val sess = current getOrElse appContext.sessions.newSession
        request(SessionSupport.SessionKey) = sess
        if (current.isEmpty) request.cookies += appContext.sessionIdKey -> sess.id
        sess
    }
  }
}
