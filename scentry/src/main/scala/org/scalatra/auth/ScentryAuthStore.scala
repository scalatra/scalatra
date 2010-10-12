package org.scalatra.auth

import javax.servlet.http.{HttpSession}
import org.scalatra.{CookieOptions, RichCookies}

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get: String
    def set(value: String): Unit
    def invalidate: Unit
  }

  class HttpOnlyCookieAuthStore(app: ScalatraKernelProxy, secureOnly: Boolean = false) extends ScentryAuthStore {


    private val SET_COOKIE = "Set-Cookie"
    private lazy val cookies: RichCookies = new RichCookies(app.cookies, app.response)

    def get: String = {
      cookies(Scentry.scentryAuthKey)
    }
    def set(value: String) {
      app.response.addHeader(SET_COOKIE, buildCookieString(value))
    }
    def invalidate {
      app.response.addHeader(SET_COOKIE, buildInvalidateCookieString)
    }

    private def buildCookieString(value: String) = {
      val sb = new StringBuffer
      sb append Scentry.scentryAuthKey
      sb append "="
      sb append value
      // I couldn't figure out how to use the classes for this so building my own string. It's been around for years now
      sb append "; HttpOnly"
      if (secureOnly) sb append "; secure"
      sb.toString
    }

    private def buildInvalidateCookieString = {
      val sb = new StringBuffer
      sb append Scentry.scentryAuthKey
      sb append "="
      // I couldn't figure out how to use the classes for this so building my own string. It's been around for years now
      sb append "; HttpOnly; Max-Age=0"
      if (secureOnly) sb append "; secure"
      sb.toString
    }

  }

  class CookieAuthStore(cookies: => RichCookies, secureOnly: Boolean = false) extends ScentryAuthStore {

    def get: String = {
      cookies(Scentry.scentryAuthKey)
    }
    def set(value: String): Unit = {
      cookies.set(Scentry.scentryAuthKey, value, CookieOptions(secure = secureOnly))
    }
    def invalidate: Unit = {
      cookies.set(Scentry.scentryAuthKey, "", CookieOptions(maxAge = 0))
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
