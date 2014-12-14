package org.scalatra
package servlet

import javax.servlet.http.HttpSession

/**
 * Extension methods to the standard HttpSession.
 */
case class RichSession(session: HttpSession) extends AttributesMap {
  def id = session.getId

  protected def attributes = session
}
