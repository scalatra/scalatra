package org.scalatra
package servlet

import javax.servlet.http.HttpSession

object ServletApiImplicits {
  implicit def enrichSession(session: HttpSession): ServletSession =
    ServletSession(session)
}
