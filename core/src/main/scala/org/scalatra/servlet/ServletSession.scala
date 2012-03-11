package org.scalatra
package servlet

import javax.servlet.http.HttpSession

object ServletSession {
  def apply(session: HttpSession) = new ServletSession(session)
}

/**
 * Extension methods to the standard HttpSession.
 */
class ServletSession(session: HttpSession)
  extends SessionWrapper(session)
  with Session 
  with AttributesMap 
{
  def id = session.getId

  protected def attributes = session
}
