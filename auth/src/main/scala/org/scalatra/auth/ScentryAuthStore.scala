package org.scalatra
package auth

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest, HttpSession }
import servlet.ServletApiImplicits._
import util.RicherString._
import java.text.SimpleDateFormat
import java.util.{ Date, TimeZone }

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get(implicit request: HttpServletRequest, response: HttpServletResponse): String
    def set(value: String)(implicit request: HttpServletRequest, response: HttpServletResponse)
    def invalidate()(implicit request: HttpServletRequest, response: HttpServletResponse)
  }

  class CookieAuthStore(app: ScalatraContext)(implicit cookieOptions: CookieOptions = CookieOptions(path = "/")) extends ScentryAuthStore {

    def get(implicit request: HttpServletRequest, response: HttpServletResponse) = app.cookies.get(Scentry.scentryAuthKey) getOrElse ""

    def set(value: String)(implicit request: HttpServletRequest, response: HttpServletResponse) {
      app.cookies.update(Scentry.scentryAuthKey, value)(cookieOptions)
    }

    def invalidate()(implicit request: HttpServletRequest, response: HttpServletResponse) {
      app.cookies.delete(Scentry.scentryAuthKey)(cookieOptions)
    }

  }

  class SessionAuthStore(app: ScalatraContext) extends ScentryAuthStore {

    def get(implicit request: HttpServletRequest, response: HttpServletResponse): String = {
      app.session.get(Scentry.scentryAuthKey).map(_.asInstanceOf[String]).orNull
    }
    def set(value: String)(implicit request: HttpServletRequest, response: HttpServletResponse) {
      app.session(Scentry.scentryAuthKey) = value
    }
    def invalidate()(implicit request: HttpServletRequest, response: HttpServletResponse) {
      app.session.invalidate()
    }
  }
}
