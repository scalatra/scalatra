package org.scalatra

import javax.servlet.http.{HttpServletResponse, Cookie => ServletCookie}
import collection._
import java.net.{URLDecoder, URLEncoder}

case class CookieOptions(
        domain  : String  = "",
        path    : String  = "",
        maxAge  : Int     = -1,
        secure  : Boolean = false,
        comment : String  = "",
        httpOnly: Boolean = false,
        encoding: String  = "UTF-8")

case class Cookie(name: String, value: Seq[String], options: CookieOptions) {

  private class RicherString(orig: String) {
    def blank_? = orig == null || orig.trim.isEmpty
    def nonBlank_? = orig != null && !orig.trim.isEmpty
  }

  private implicit def string2RicherString(orig: String) = new RicherString(orig)

  def toCookieString = {
    val sb = new StringBuffer
    sb append (URLEncoder.encode(name, options.encoding)) append "="
    sb append (value.map(URLEncoder.encode(_, options.encoding)).mkString("&"))

    if(options.domain.nonBlank_?) sb.append("; Domain=").append(
      if (!options.domain.startsWith(".")) "." + options.domain else options.domain
    )

    val pth = if(options.path.blank_?) Cookie.contextPath else options.path
    if(pth.nonBlank_?) sb append "; Path=" append (if(!pth.startsWith("/")) {
      "/" + pth
    } else { pth })

    if(options.comment.nonBlank_?) sb append ("; Comment=") append options.comment

    if(options.maxAge > -1) sb append "; Max-Age=" append options.maxAge

    if (options.secure) sb append "; Secure"
    if (options.httpOnly) sb append "; HttpOnly"
    sb.toString
  }
}

object Cookie {

  private var _contextPath: () => String = () => ""
  def requestContextPath(pth: => String) {
    _contextPath = () => pth
  }
  def contextPath = _contextPath()

  def apply(name: String, value: String*): Cookie = new Cookie(name, value, CookieOptions())

  def apply(name: String, value: String, options: CookieOptions = CookieOptions()): Cookie =
    Cookie(name, Array(value), options)

}

class RichCookies(cookieColl: Array[ServletCookie], response: HttpServletResponse) {

  def get(key: String) = cookieColl.find(_.getName == key) match {
      case Some(cookie) => Some(URLDecoder.decode(cookie.getValue, "UTF-8"))
      case _ => None
  }

  def apply(key: String) = get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))

  def update(name: String, value: String, options: CookieOptions = CookieOptions()) = {
    val cookie = Cookie(name, value, options)
    response.addHeader("Set-Cookie", cookie.toCookieString)
    cookie
  }

  def set(name: String, value: String, options: CookieOptions = CookieOptions()) = {
    this.update(name, value, options)
  }
}

trait CookieSupport {

  self: ScalatraKernel =>

  protected implicit def cookieWrapper(cookieColl: Array[ServletCookie]) = new RichCookies(cookieColl, response)

  protected def cookies = request.getCookies match {
    case null => Array[ServletCookie]()
    case x => x
  }

  Cookie.requestContextPath( request.getContextPath)



}
