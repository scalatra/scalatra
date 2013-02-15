package org.scalatra
package auth

import javax.servlet.http.{HttpServletRequest, HttpSession}
import servlet.ServletApiImplicits._
import util.RicherString._
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get(implicit request: HttpServletRequest): String
    def set(value: String)(implicit request: HttpServletRequest)
    def invalidate()(implicit request: HttpServletRequest)
  }

  class CookieAuthStore(app: ScalatraContext)(implicit cookieOptions: CookieOptions = CookieOptions(path = "/")) extends ScentryAuthStore {

    def get(implicit request: HttpServletRequest) = app.cookies.get(Scentry.scentryAuthKey) getOrElse ""

    def set(value: String)(implicit request: HttpServletRequest) {
      app.cookies.update(Scentry.scentryAuthKey, value)(cookieOptions)
    }

    def invalidate()(implicit request: HttpServletRequest) {
      app.cookies.delete(Scentry.scentryAuthKey)(cookieOptions)
    }

  }

  class SessionAuthStore(app: ScalatraContext) extends ScentryAuthStore {

    def get(implicit request: HttpServletRequest): String = {
      app.session.get(Scentry.scentryAuthKey).map(_.asInstanceOf[String]).orNull
    }
    def set(value: String)(implicit request: HttpServletRequest) {
      app.session(Scentry.scentryAuthKey) = value
    }
    def invalidate()(implicit request: HttpServletRequest) {
      app.session.invalidate()
    }
  }
}
