package org.scalatra

import java.util.{ Date, Locale }
import javax.servlet.http.HttpServletResponse

import org.scalatra.servlet.ServletApiImplicits
import org.scalatra.util.DateUtil
import org.scalatra.util.RicherString._

import scala.collection._

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

  def currentTimeMillis: Long = _currentTimeMillis getOrElse System.currentTimeMillis

  def currentTimeMillis_=(ct: Long): Unit = _currentTimeMillis = Some(ct)

  def freezeTime(): Unit = _currentTimeMillis = Some(System.currentTimeMillis())

  def unfreezeTime(): Unit = _currentTimeMillis = None

  def formatExpires(date: Date): String = DateUtil.formatDate(date, "EEE, dd MMM yyyy HH:mm:ss zzz")

}
case class Cookie(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
  import org.scalatra.Cookie._

  val options: CookieOptions = cookieOptions

  def toCookieString: String = {
    val sb = new StringBuilder
    sb append name append "="
    sb append value

    if (cookieOptions.domain.nonBlank && cookieOptions.domain != "localhost")
      sb.append("; Domain=").append({
        if (!cookieOptions.domain.startsWith(".")) "." + cookieOptions.domain
        else cookieOptions.domain
      }.toLowerCase(Locale.ENGLISH))

    val pth = cookieOptions.path
    if (pth.nonBlank) {
      sb append "; Path=" append (if (!pth.startsWith("/")) "/" + pth else pth)
    }
    if (cookieOptions.comment.nonBlank) {
      sb append ("; Comment=") append cookieOptions.comment
    }

    appendMaxAge(sb, cookieOptions.maxAge, cookieOptions.version)

    if (cookieOptions.secure) sb append "; Secure"
    if (cookieOptions.httpOnly) sb append "; HttpOnly"
    sb.toString
  }

  private[this] def appendMaxAge(sb: StringBuilder, maxAge: Int, version: Int): StringBuilder = {
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

  private[this] def appendExpires(sb: StringBuilder, expires: Date): StringBuilder =
    sb append "; Expires=" append formatExpires(expires)
}

class SweetCookies(
    private[this] val reqCookies: Map[String, String],
    private[this] val response: HttpServletResponse) extends ServletApiImplicits {

  private[this] lazy val cookies = mutable.HashMap[String, String]() ++ reqCookies

  def get(key: String): Option[String] = cookies.get(key)

  def apply(key: String): String = {
    cookies.get(key) getOrElse (throw new Exception("No cookie could be found for the specified key"))
  }

  def update(name: String, value: String)(
    implicit cookieOptions: CookieOptions = CookieOptions()): Cookie = {
    cookies += name -> value
    addCookie(name, value, cookieOptions)
  }

  def set(name: String, value: String)(
    implicit cookieOptions: CookieOptions = CookieOptions()): Cookie = {
    this.update(name, value)(cookieOptions)
  }

  def delete(name: String)(implicit cookieOptions: CookieOptions = CookieOptions()): Unit = {
    cookies -= name
    addCookie(name, "", cookieOptions.copy(maxAge = 0))
  }

  def +=(keyValuePair: (String, String))(
    implicit cookieOptions: CookieOptions = CookieOptions()): Cookie = {
    this.update(keyValuePair._1, keyValuePair._2)(cookieOptions)
  }

  def -=(key: String)(implicit cookieOptions: CookieOptions = CookieOptions()): Unit = {
    delete(key)(cookieOptions)
  }

  private def addCookie(name: String, value: String, options: CookieOptions): Cookie = {
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

  import org.scalatra.CookieSupport._

  implicit def cookieOptions: CookieOptions = {
    servletContext.get(CookieOptionsKey).orNull.asInstanceOf[CookieOptions]
  }

  def cookies: SweetCookies = {
    request.get(SweetCookiesKey).orNull.asInstanceOf[SweetCookies]
  }

}
@deprecated("You can remove this mixin, it's included in core by default", "2.2")
trait CookieSupport { self: ScalatraBase =>
}
