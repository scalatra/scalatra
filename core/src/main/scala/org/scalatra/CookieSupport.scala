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

private[scalatra] class RicherString(orig: String) {
    def isBlank = orig == null || orig.trim.isEmpty
    def isNonBlank = orig != null && !orig.trim.isEmpty
  }
case class Cookie(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {



  private implicit def string2RicherString(orig: String) = new RicherString(orig)

  def toServletCookie = {
    val sCookie = new ServletCookie(name, value)
    if(cookieOptions.domain.isNonBlank) sCookie.setDomain(cookieOptions.domain)
    if(cookieOptions.path.isNonBlank) sCookie.setPath(cookieOptions.path)
    sCookie.setMaxAge(cookieOptions.maxAge)
    if(cookieOptions.secure) sCookie.setSecure(cookieOptions.secure)
    if(cookieOptions.comment.isNonBlank) sCookie.setComment(cookieOptions.comment)
    sCookie
  }

  def toCookieString = {
    val sb = new StringBuffer
    sb append name append "="
    sb append value

    if(cookieOptions.domain.isNonBlank) sb.append("; Domain=").append(
      (if (!cookieOptions.domain.startsWith(".")) "." + cookieOptions.domain else cookieOptions.domain).toLowerCase(Locale.ENGLISH)
    )

    val pth = cookieOptions.path
    if(pth.isNonBlank) sb append "; Path=" append (if(!pth.startsWith("/")) {
      "/" + pth
    } else { pth })

    if(cookieOptions.comment.isNonBlank) sb append ("; Comment=") append cookieOptions.comment

    if(cookieOptions.maxAge > -1) sb append "; Max-Age=" append cookieOptions.maxAge

    if (cookieOptions.secure) sb append "; Secure"
    if (cookieOptions.httpOnly) sb append "; HttpOnly"
    sb.toString
  }
}


class SweetCookies(cookieColl: Array[ServletCookie], response: HttpServletResponse) {

  private implicit def string2RicherString(orig: String) = new RicherString(orig)
  private val cookies = new mutable.HashMap[String, String]()
  cookieColl.foreach { ck =>
    cookies += (ck.getName -> ck.getValue)
  }

  def get(key: String) = cookies.get(key)

  def apply(key: String) = cookies.get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))

  def update(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    val sCookie = new ServletCookie(name, value)
    if(cookieOptions.domain.isNonBlank) sCookie.setDomain(cookieOptions.domain)
    if(cookieOptions.path.isNonBlank) sCookie.setPath(cookieOptions.path)
    sCookie.setMaxAge(cookieOptions.maxAge)
    if(cookieOptions.secure) sCookie.setSecure(cookieOptions.secure)
    if(cookieOptions.comment.isNonBlank) sCookie.setComment(cookieOptions.comment)
    cookies += name -> value
    //response.addHeader("Set-Cookie", cookie.toCookieString)
    response.addCookie(sCookie)
    sCookie
  }

  def set(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    this.update(name, value)(cookieOptions)
  }

  def delete(name: String) {
    cookies -= name
    response.addHeader("Set-Cookie", Cookie(name, "")(CookieOptions(maxAge = 0)).toCookieString)
  }

  def +=(keyValuePair: (String, String))(options: CookieOptions) = {
    update(keyValuePair._1, keyValuePair._2)(options)
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

  implicit val cookieOptions: CookieOptions = CookieOptions(path=request.getContextPath)

  protected def cookies = request.getCookies match {
    case null => Array[ServletCookie]()
    case x => x
  }




}
