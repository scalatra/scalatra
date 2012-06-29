package org.scalatra

import collection._
import java.util.Locale
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Cookie => ServletCookie}
import servlet.ServletApiImplicits
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
  val options = cookieOptions

  def toCookieString = {
    val sb = new StringBuffer
    sb append name append "="
    sb append value

    if(cookieOptions.domain.nonBlank) sb.append("; Domain=").append(
      (if (!cookieOptions.domain.startsWith(".")) "." + cookieOptions.domain else cookieOptions.domain).toLowerCase(Locale.ENGLISH)
    )

    val pth = cookieOptions.path
    if(pth.nonBlank) sb append "; Path=" append (if(!pth.startsWith("/")) {
      "/" + pth
    } else { pth })

    if(cookieOptions.comment.nonBlank) sb append ("; Comment=") append cookieOptions.comment

    if(cookieOptions.maxAge > -1) sb append "; Max-Age=" append cookieOptions.maxAge

    if (cookieOptions.secure) sb append "; Secure"
    if (cookieOptions.httpOnly) sb append "; HttpOnly"
    sb.toString
  }
}

class SweetCookies(private val reqCookies: Map[String, String], private val response: HttpServletResponse) extends ServletApiImplicits {
  private lazy val cookies = mutable.HashMap[String, String]() ++ reqCookies

  def get(key: String) = cookies.get(key)

  def apply(key: String) = cookies.get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))

  def update(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    cookies += name -> value
    addCookie(name, value, cookieOptions)
  }

  def set(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    this.update(name, value)(cookieOptions)
  }

  def delete(name: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
    cookies -= name
    addCookie(name, "", cookieOptions.copy(maxAge = 0))
  }

  def +=(keyValuePair: (String, String))(implicit cookieOptions: CookieOptions = CookieOptions()) = {
    this.update(keyValuePair._1, keyValuePair._2)(cookieOptions)
  }

  def -=(key: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
    delete(key)(cookieOptions)
  }

  private def addCookie(name: String, value: String, options: CookieOptions) = {
    val cookie = new Cookie(name, value)(options)
    response.addCookie(cookie)
    cookie
  }
}

object CookieSupport {
  val SweetCookiesKey = "org.scalatra.SweetCookies".intern
  val CookieOptionsKey = "org.scalatra.CookieOptions".intern
}
trait CookieSupport extends Handler {
  self: ScalatraBase =>

  import CookieSupport._
  implicit def cookieOptions: CookieOptions = request(CookieOptionsKey).asInstanceOf[CookieOptions]

  def cookies = request(SweetCookiesKey).asInstanceOf[SweetCookies]

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    req(SweetCookiesKey) = new SweetCookies(req.cookies, res)
    val path = contextPath match {
      case "" => "/" // The root servlet is "", but the root cookie path is "/"
      case p => p
    }
    req(CookieOptionsKey) = CookieOptions(path = path)
    super.handle(req, res)
  }

}
