package org.scalatra

import collection._
import java.util.Locale
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Cookie => ServletCookie}
import scala.util.DynamicVariable
import util.RicherString._

case class CookieOptions(
        domain  : String  = "",
        path    : String  = "",
        maxAge  : Int     = -1,
        secure  : Boolean = false,
        comment : String  = "",
        httpOnly: Boolean = false,
        encoding: String  = "UTF-8")

case class Cookie(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
  def toServletCookie = {
    val sCookie = new ServletCookie(name, value)
    if(cookieOptions.domain.isNonBlank) sCookie.setDomain(cookieOptions.domain)
    if(cookieOptions.path.isNonBlank) sCookie.setPath(cookieOptions.path)
    sCookie.setMaxAge(cookieOptions.maxAge)
    if(cookieOptions.secure) sCookie.setSecure(cookieOptions.secure)
    if(cookieOptions.comment.isNonBlank) sCookie.setComment(cookieOptions.comment)
    sCookie.setHttpOnly(cookieOptions.httpOnly)
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

class SweetCookies(private val reqCookies: Map[String, String], private val response: HttpServletResponse) {
  private lazy val cookies = mutable.HashMap[String, String]() ++ reqCookies

  def get(key: String) = cookies.get(key)

  def apply(key: String) = cookies.get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))

  def update(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    cookies += name -> value
    addServletCookie(name, value, cookieOptions.copy(maxAge = 0))
  }

  def set(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    this.update(name, value)(cookieOptions)
  }

  def delete(name: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
    cookies -= name
    addServletCookie(name, "", cookieOptions.copy(maxAge = 0))
  }

  def +=(keyValuePair: (String, String))(implicit cookieOptions: CookieOptions = CookieOptions()) = {
    this.update(keyValuePair._1, keyValuePair._2)(cookieOptions)
  }

  def -=(key: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
    delete(key)(cookieOptions)
  }

  private def addServletCookie(name: String, value: String, options: CookieOptions) = {
    val cookie = new Cookie(name, value)(options)
    val servletCookie = cookie.toServletCookie
    response.addCookie(servletCookie)
    servletCookie
  }
}

object CookieSupport {
  val SweetCookiesKey = "org.scalatra.SweetCookies".intern
  val CookieOptionsKey = "org.scalatra.CookieOptions".intern
}
trait CookieSupport extends Handler {
  self: ScalatraKernel =>

  import CookieSupport._
  implicit def cookieOptions: CookieOptions = request(CookieOptionsKey).asInstanceOf[CookieOptions]

  def cookies = request(SweetCookiesKey).asInstanceOf[SweetCookies]

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    req(SweetCookiesKey) = new SweetCookies(req.cookies, res)
    req(CookieOptionsKey) = CookieOptions(path = req.getContextPath)
    super.handle(req, res)
  }

}
