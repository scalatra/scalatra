package org.scalatra.auth

import javax.servlet.http.{HttpSession}
import org.scalatra.{CookieOptions, SweetCookies}

object ScentryAuthStore {

  trait ScentryAuthStore {
    def get: String
    def set(value: String): Unit
    def invalidate: Unit
  }

  class HttpOnlyCookieAuthStore(cookies: => SweetCookies, secureOnly: Boolean = false) extends CookieAuthStore(cookies, secureOnly) {


//    private val SET_COOKIE = "Set-Cookie"
//    private lazy val cookies: SweetCookies = new SweetCookies(app.cookies, app.response)

//    def get: String = {
//      cookies.getFirst(Scentry.scentryAuthKey)
//    }
    override def set(value: String) {
      cookies.set(Scentry.scentryAuthKey, value, CookieOptions(secure = secureOnly, httpOnly = true))
    }

//    def invalidate {
//      app.response.addHeader(SET_COOKIE, buildInvalidateCookieString)
//    }

//    private def buildCookieString(value: String) = {
//      val sb = new StringBuffer
//      sb append Scentry.scentryAuthKey
//      sb append "="
//      sb append value
//      // I couldn't figure out how to use the classes for this so building my own string. It's been around for years now
//      sb append "; HttpOnly"
//      if (secureOnly) sb append "; secure"
//      sb.toString
//    }
//
//    private def buildInvalidateCookieString = {
//      val sb = new StringBuffer
//      sb append Scentry.scentryAuthKey
//      sb append "="
//      // I couldn't figure out how to use the classes for this so building my own string. It's been around for years now
//      sb append "; HttpOnly; Max-Age=0"
//      if (secureOnly) sb append "; secure"
//      sb.toString
//    }

  }

  class CookieAuthStore(cookies: => SweetCookies, secureOnly: Boolean = false) extends ScentryAuthStore {

    def get: String = {
      cookies(Scentry.scentryAuthKey).toString
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
