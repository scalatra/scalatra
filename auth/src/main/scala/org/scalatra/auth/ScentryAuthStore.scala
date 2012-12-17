package org.scalatra
package auth

import javax.servlet.http.HttpSession
import servlet.ServletApiImplicits._
import util.RicherString._
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get: String
    def set(value: String)
    def invalidate()
  }

  class CookieAuthStore(app: ⇒ ScalatraSyntax with CookieSupport)(implicit cookieOptions: CookieOptions = CookieOptions(path = "/")) extends ScentryAuthStore {

    def get = app.cookies.get(Scentry.scentryAuthKey) getOrElse ""

    def set(value: String) {
      app.cookies.update(Scentry.scentryAuthKey, value)(cookieOptions)
    }

    def invalidate() {
      app.cookies.delete(Scentry.scentryAuthKey)(cookieOptions)
    }

  }

  class SessionAuthStore(session: ⇒ HttpSession) extends ScentryAuthStore {

    def get: String = {
      session.get(Scentry.scentryAuthKey).map(_.asInstanceOf[String]).orNull
    }
    def set(value: String) {
      session(Scentry.scentryAuthKey) = value
    }
    def invalidate() {
      session.invalidate()
    }
  }
}
