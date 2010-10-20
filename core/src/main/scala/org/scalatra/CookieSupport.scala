package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Cookie}
import javax.servlet.ServletContext

case class CookieOptions(
        domain  : String  = "",
        path    : String  = "",
        maxAge  : Int     = -1,
        secure  : Boolean = false,
        comment : String  = "")


class RichCookies(cookieColl: Array[Cookie], response: HttpServletResponse) {
    def apply(key: String) = cookieColl.find(_.getName == key) match {
      case Some(cookie) => Some(cookie.getValue)
      case _ => None
    }
    def apply(key: Symbol): Option[String] = apply(key.name)

    def isNotBlank(s:String): Boolean = s != null && s.trim.length > 0
    def update(name: String, value: String, options: CookieOptions = CookieOptions()) = {
      val cookie = new Cookie(name, value)
      if (isNotBlank(options.domain)) cookie.setDomain(options.domain)
      if (isNotBlank(options.path)) cookie.setPath(options.path)
      if (options.secure) cookie.setSecure(true)
      if (isNotBlank(options.comment)) cookie.setComment(options.comment)
      cookie.setMaxAge(options.maxAge)

      response addCookie cookie
      cookie
    }
    def set(name: String, value: String, options: CookieOptions = CookieOptions()) = {
      this.update(name, value, options)
    }
  }

trait CookieSupport {

  self: ScalatraKernel =>

  protected implicit def cookieWrapper(cookieColl: Array[Cookie]) = new RichCookies(cookieColl, response)

  protected def cookies = request.getCookies match {
    case null => Array[Cookie]()
    case x => x
  }



}
