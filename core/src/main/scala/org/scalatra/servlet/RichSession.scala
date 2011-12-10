package org.scalatra
package servlet

import javax.servlet.http.HttpSession

/**
 * Extension methods to the standard HttpSession.
 */
class RichSession(session: HttpSession) extends AttributesMap {
  protected def attributes = session
}
