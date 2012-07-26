package org.scalatra

import collection._
import java.util.{Calendar, TimeZone, Date, Locale}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Cookie => ServletCookie}
import servlet.ServletApiImplicits
import util.RicherString._
import java.text.SimpleDateFormat

case class CookieOptions(
        domain  : String  = "",
        path    : String  = "",
        maxAge  : Int     = -1,
        expires : Option[Date] = None,
        secure  : Boolean = false,
        comment : String  = "",
        httpOnly: Boolean = false,
        encoding: String  = "UTF-8")

object Cookie {
  private[this] var _currentTimeMillis: Option[Long] = None
  def currentTimeMillis = _currentTimeMillis getOrElse System.currentTimeMillis
  def currentTimeMillis_=(ct: Long) = synchronized { _currentTimeMillis = Some(ct) }
  def freezeTime() = synchronized { _currentTimeMillis = Some(System.currentTimeMillis()) }
  def unfreezeTime() = synchronized { _currentTimeMillis = None }
  def formatExpires(date: Date) = {
    val df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    df.setTimeZone(TimeZone.getTimeZone("GMT"))
    df.format(date)
  }
}
case class Cookie(name: String, value: String)(implicit cookieOptions: CookieOptions = CookieOptions()) {
  import Cookie._

  val options = cookieOptions

  def toCookieString = {
    val sb = new StringBuffer
    sb append name append "="
    sb append value

    if(cookieOptions.domain.nonBlank && cookieOptions.domain != "localhost")
      sb.append("; Domain=").append(
        (if (!cookieOptions.domain.startsWith(".")) "." + cookieOptions.domain else cookieOptions.domain).toLowerCase(Locale.ENGLISH)
      )

    val pth = cookieOptions.path
    if(pth.nonBlank) sb append "; Path=" append (if(!pth.startsWith("/")) {
      "/" + pth
    } else { pth })

    if(cookieOptions.comment.nonBlank) sb append ("; Comment=") append cookieOptions.comment

    if (cookieOptions.expires.isEmpty)
      // This used to be Max-Age but IE is not always very happy with that
      cookieOptions.maxAge match {
        case a if a < 0 => // we don't do anything for max-age when it's < 0 then it becomes a session cookie
        case 0 => appendMaxAge(sb, 0) // count 1 backwards because we want to make the cookie go away fo sho
        case a => appendMaxAge(sb, currentTimeMillis + a * 1000)
      }
    else
      cookieOptions.expires foreach (appendExpires(sb, _))

    if (cookieOptions.secure) sb append "; Secure"
    if (cookieOptions.httpOnly) sb append "; HttpOnly"
    sb.toString
  }

  private[this] def appendMaxAge(sb: StringBuffer, dateInMillis: Long) = {
    appendExpires(sb, new Date(dateInMillis))
  }

  private[this] def appendExpires(sb: StringBuffer, expires: Date) =
    sb append  "; Expires=" append formatExpires(expires)
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
  val SweetCookiesKey = "org.scalatra.SweetCookies"
  val CookieOptionsKey = "org.scalatra.CookieOptions"
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
