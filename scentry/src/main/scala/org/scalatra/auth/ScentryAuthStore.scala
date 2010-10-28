package org.scalatra.auth

import javax.servlet.http.{HttpServletResponse, HttpSession}
import org.scalatra.{Cookie, CookieOptions, SweetCookies}

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get: String
    def set(value: String): Unit
    def invalidate: Unit
  }

  class HttpOnlyCookieAuthStore(app: => ScalatraKernelProxy, secureOnly: Boolean = false) extends CookieAuthStore(app.cookies, secureOnly) {


    private val SET_COOKIE = "Set-Cookie"

    override def set(value: String) {
      app.response.addHeader(SET_COOKIE, Cookie(Scentry.scentryAuthKey, value)(CookieOptions(secure = secureOnly, httpOnly = true)).toCookieString)
    }

  }

  class CookieAuthStore(cookies: => SweetCookies, secureOnly: Boolean = false) extends ScentryAuthStore {

    def get: String = {
      cookies(Scentry.scentryAuthKey).toString
    }
    def set(value: String): Unit = {
      cookies.set(Scentry.scentryAuthKey, value)(CookieOptions(secure = secureOnly))
    }
    def invalidate: Unit = {
      cookies -= Scentry.scentryAuthKey
    }
  }

  class SessionAuthStore(session: => HttpSession) extends ScentryAuthStore{

    def get: String = {
      session.getAttribute(Scentry.scentryAuthKey).asInstanceOf[String]
    }
    def set(value: String): Unit = {
      session.setAttribute(Scentry.scentryAuthKey, value)
    }
    def invalidate: Unit = session.invalidate
  }
}
