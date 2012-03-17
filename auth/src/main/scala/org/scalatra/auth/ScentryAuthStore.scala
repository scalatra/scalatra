package org.scalatra
package auth

import servlet.ServletBase

import javax.servlet.http.HttpSession

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get: String
    def set(value: String)
    def invalidate
  }

  class HttpOnlyCookieAuthStore(app: => (ServletBase with CookieSupport), secureOnly: Boolean = false)
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

  class CookieAuthStore(cookies: => SweetCookies, secureOnly: Boolean = false) extends ScentryAuthStore {

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

  class SessionAuthStore(session: => HttpSession) extends ScentryAuthStore{

    def get: String = {
      session.getAttribute(Scentry.scentryAuthKey).asInstanceOf[String]
    }
    def set(value: String) {
      session.setAttribute(Scentry.scentryAuthKey, value)
    }
    def invalidate {
      session.invalidate()
    }
  }
}
