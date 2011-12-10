package org.scalatra

import javax.servlet.http.HttpSession
import util.MapWithIndifferentAccess

/**
 * Extension methods to the standard HttpSession.
 */
class RichSession(session: HttpSession) extends AttributesMap {
  protected def attributes = session
}
