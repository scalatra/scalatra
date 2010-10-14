package org.scalatra

import javax.servlet.http.HttpSession

class RichSession(session: HttpSession) extends AttributesMap {
  protected def attributes = session
}
