package org.scalatra

import collection._
import java.util.Locale
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Cookie => ServletCookie}
import util.DynamicVariable

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

object SweetCookies {
  private var _cookies: mutable.HashMap[String, String] = null

  private def fillCookieJar(cookieColl: Array[ServletCookie]) = {
    if(_cookies == null) {
      _cookies = new mutable.HashMap[String, String]()
      cookieColl.foreach { ck =>
        _cookies += (ck.getName -> ck.getValue)
      }
      _cookies
    } else _cookies
  }
}

class SweetCookies(cookieColl: Array[ServletCookie], response: HttpServletResponse) {

  import SweetCookies._
  private implicit def string2RicherString(orig: String) = new RicherString(orig)
  private val cookies = fillCookieJar(cookieColl)

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

  def +=(keyValuePair: (String, String))(implicit cookieOptions: CookieOptions) = {
    update(keyValuePair._1, keyValuePair._2)(cookieOptions)
  }

  def +=(keyValuePair: (String, String)) = {
    update(keyValuePair._1, keyValuePair._2)
  }

  def -=(key: String) {
    delete(key)
  }
}

trait CookieSupport extends Handler {

  self: ScalatraKernel =>

  implicit def cookieOptions: CookieOptions = _cookieOptions.value

  protected def cookies = _cookies.value

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    _cookies.withValue(new SweetCookies(req.getCookies, res)) {
      _cookieOptions.withValue(CookieOptions(path = req.getContextPath)) {
        super.handle(req, res)
      }
    }
  }

  private val _cookies = new DynamicVariable[SweetCookies](null)
  private val _cookieOptions = new DynamicVariable[CookieOptions](CookieOptions())


}
