package org.scalatra
package auth

import servlet.ServletBase

import javax.servlet.http.HttpSession
import sun.plugin2.main.server.CookieSupport

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get: String
    def set(value: String)
    def invalidate
  }

  class HttpOnlyCookieAuthStore(app: => (ScalatraApp with CookieSupport), secureOnly: Boolean = false)
      extends CookieAuthStore(app.cookies, secureOnly) {

    private val SET_COOKIE = "Set-Cookie".intern

    override def set(value: String) {

      //TODO: Make use of servlet 3.0 cookie implementation
      app.response.addHeader(
        SET_COOKIE,
        Cookie(Scentry.scentryAuthKey, value)(CookieOptions(secure = secureOnly, httpOnly = true)).toCookieString
      )
    }

  }

  class CookieAuthStore(cookies: => CookieJar, secureOnly: Boolean = false)(implicit appContext: AppContext) extends ScentryAuthStore {

    def get: String = {
      cookies.get(Scentry.scentryAuthKey) getOrElse ""
    }
    def set(value: String) {
      cookies.set(Scentry.scentryAuthKey, value)(CookieOptions(secure = secureOnly))
    }
    def invalidate {
      cookies -= Scentry.scentryAuthKey
    }
  }

  class SessionAuthStore(session: => HttpSession)(implicit appContext: AppContext) extends ScentryAuthStore{

    def get: String = {
      session.get(Scentry.scentryAuthKey).map(_.toString).orNull
    }
    def set(value: String) {
      session(Scentry.scentryAuthKey) = value
    }
    def invalidate {

    }
  }
}
