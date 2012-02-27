package org.scalatra
package servlet

import javax.servlet.http.{HttpSession => ServletSession}

/**
 * Extension methods to the standard HttpSession.
 */
class RichSession(session: ServletSession) extends HttpSession with AttributesMap {
  def id = session.getId

  protected def attributes = session

  def invalidate() = session.invalidate()
}
