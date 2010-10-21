package org.scalatra

import javax.servlet.http.{HttpServletResponse, Cookie => ServletCookie}
import collection._
import java.util.Locale

case class CookieOptions(
        domain  : String  = "",
        path    : String  = "",
        maxAge  : Int     = -1,
        secure  : Boolean = false,
        comment : String  = "",
        httpOnly: Boolean = false,
        encoding: String  = "UTF-8")

case class Cookie(name: String, value: String, options: CookieOptions = CookieOptions()) {

  private class RicherString(orig: String) {
    def isBlank = orig == null || orig.trim.isEmpty
    def isNonBlank = orig != null && !orig.trim.isEmpty
  }

  private implicit def string2RicherString(orig: String) = new RicherString(orig)

  def toServletCookie = {
    val sCookie = new ServletCookie(name, value)
    if(options.domain.isNonBlank) sCookie.setDomain(options.domain)
    if(options.path.isNonBlank) sCookie.setPath(options.path)
    sCookie.setMaxAge(options.maxAge)
    if(options.secure) sCookie.setSecure(options.secure)
    if(options.comment.isNonBlank) sCookie.setComment(options.comment)
    sCookie
  }

  def toCookieString = {
    val sb = new StringBuffer
    sb append name append "="
    sb append value

    if(options.domain.isNonBlank) sb.append("; Domain=").append(
      (if (!options.domain.startsWith(".")) "." + options.domain else options.domain).toLowerCase(Locale.ENGLISH)
    )

    val pth = if(options.path.isBlank) Cookie.contextPath else options.path
    if(pth.isNonBlank) sb append "; Path=" append (if(!pth.startsWith("/")) {
      "/" + pth
    } else { pth })

    if(options.comment.isNonBlank) sb append ("; Comment=") append options.comment

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

}

class SweetCookies(cookieColl: Array[ServletCookie], response: HttpServletResponse) {

  private val cookies = new mutable.HashMap[String, String]()
  cookieColl.foreach { ck =>
    cookies += (ck.getName -> ck.getValue)
  }

  def get(key: String) = cookies.get(key)

  def apply(key: String) = cookies.get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))

  def update(name: String, value: String, options: CookieOptions=CookieOptions()): Cookie = {
    val sCookie = new ServletCookie(name, value)
    if(options.domain.isNonBlank) sCookie.setDomain(options.domain)
    if(options.path.isNonBlank) sCookie.setPath(options.path)
    sCookie.setMaxAge(options.maxAge)
    if(options.secure) sCookie.setSecure(options.secure)
    if(options.comment.isNonBlank) sCookie.setComment(options.comment)
    cookies += name -> value
    //response.addHeader("Set-Cookie", cookie.toCookieString)
    response.addCookie(sCookie)
    sCookie
  }

  def set(name: String, value: String, options: CookieOptions=CookieOptions()): Cookie = {
    this.update(name, value, options)
  }

  def delete(name: String) {
    cookies -= name
    response.addHeader("Set-Cookie", Cookie(name, "", CookieOptions(maxAge = 0)).toCookieString)
  }

  def +=(keyValuePair: (String, String))(options: CookieOptions) = {
    update(keyValuePair._1, keyValuePair._2, options)
  }

  def +=(keyValuePair: (String, String)) = {
    update(keyValuePair._1, keyValuePair._2)
  }

  def -=(key: String) {
    delete(key)
  }
}

trait CookieSupport {

  self: ScalatraKernel =>

  protected implicit def cookieWrapper(cookieColl: Array[ServletCookie]) = new SweetCookies(cookieColl, response)

  protected def cookies = request.getCookies match {
    case null => Array[ServletCookie]()
    case x => x
  }

  private def ctxtPath = {
    //mainly here because it's being used outside a web context in the unit tests
    if(request != null && request.getContextPath != null) {
      request.getContextPath
    } else { "" }
  }
  Cookie.requestContextPath(ctxtPath)



}
