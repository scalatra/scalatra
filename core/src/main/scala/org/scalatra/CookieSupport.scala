package org.scalatra

import collection._
import java.util.{ Calendar, TimeZone, Date, Locale }
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse, Cookie => ServletCookie }
import servlet.ServletApiImplicits
import util.DateUtil
import util.RicherString._

case class CookieOptions(
  domain: String = "",
  path: String = "",
  maxAge: Int = -1,
  secure: Boolean = false,
  comment: String = "",
  httpOnly: Boolean = false,
  version: Int = 0,
  encoding: String = "UTF-8")

object Cookie {
  @volatile private[this] var _currentTimeMillis: Option[Long] = None
  def currentTimeMillis = _currentTimeMillis getOrElse System.currentTimeMillis
  def currentTimeMillis_=(ct: Long) = _currentTimeMillis = Some(ct)
  def freezeTime() = _currentTimeMillis = Some(System.currentTimeMillis())
  def unfreezeTime() = _currentTimeMillis = None
  def formatExpires(date: Date) = DateUtil.formatDate(date, "EEE, dd MMM yyyy HH:mm:ss zzz")
}
case class Cookie(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
  import Cookie._

  val options = cookieOptions

  def toCookieString = {
    val sb = new StringBuffer
    sb append name append "="
    sb append value

    if (cookieOptions.domain.nonBlank && cookieOptions.domain != "localhost")
      sb.append("; Domain=").append(
        (if (!cookieOptions.domain.startsWith(".")) "." + cookieOptions.domain else cookieOptions.domain).toLowerCase(Locale.ENGLISH)
      )

    val pth = cookieOptions.path
    if (pth.nonBlank) sb append "; Path=" append (if (!pth.startsWith("/")) {
      "/" + pth
    } else { pth })

    if (cookieOptions.comment.nonBlank) sb append ("; Comment=") append cookieOptions.comment

    appendMaxAge(sb, cookieOptions.maxAge, cookieOptions.version)

    if (cookieOptions.secure) sb append "; Secure"
    if (cookieOptions.httpOnly) sb append "; HttpOnly"
    sb.toString
  }

  private[this] def appendMaxAge(sb: StringBuffer, maxAge: Int, version: Int) = {
    val dateInMillis = maxAge match {
      case a if a < 0 => None // we don't do anything for max-age when it's < 0 then it becomes a session cookie
      case 0 => Some(0L) // Set the date to the min date for the system
      case a => Some(currentTimeMillis + a * 1000)
    }

    // This used to be Max-Age but IE is not always very happy with that
    // see: http://mrcoles.com/blog/cookies-max-age-vs-expires/
    // see Q1: http://blogs.msdn.com/b/ieinternals/archive/2009/08/20/wininet-ie-cookie-internals-faq.aspx
    val bOpt = dateInMillis map (ms => appendExpires(sb, new Date(ms)))
    val agedOpt = if (version > 0) bOpt map (_.append("; Max-Age=").append(maxAge)) else bOpt
    agedOpt getOrElse sb
  }

  private[this] def appendExpires(sb: StringBuffer, expires: Date) =
    sb append "; Expires=" append formatExpires(expires)
}

class SweetCookies(private[this] val reqCookies: Map[String, String], private[this] val response: HttpServletResponse) extends ServletApiImplicits {
  private[this] lazy val cookies = mutable.HashMap[String, String]() ++ reqCookies

  def get(key: String) = cookies.get(key)

  def apply(key: String) = cookies.get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))

  def update(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) = {
    cookies += name -> value
    addCookie(name, value, cookieOptions)
  }

  def set(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) = {
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
  val SweetCookiesKey = "org.scalatra.SweetCookies"
  val CookieOptionsKey = "org.scalatra.CookieOptions"
}

trait CookieContext { self: ScalatraContext =>
  import CookieSupport._
  implicit def cookieOptions: CookieOptions = servletContext.get(CookieOptionsKey).orNull.asInstanceOf[CookieOptions]

  def cookies = request.get(SweetCookiesKey).orNull.asInstanceOf[SweetCookies]

}
@deprecated("You can remove this mixin, it's included in core by default", "2.2")
trait CookieSupport { self: ScalatraBase =>
}
