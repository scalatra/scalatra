package org.scalatra

import javax.servlet.http.HttpSession
import util.MapWithIndifferentAccess

class RichSession(session: HttpSession) extends AttributesMap {
  protected def attributes = session
}
