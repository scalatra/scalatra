package org.scalatra

import collection.mutable
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._

case class CookieOptions(
        domain  : String  = "",
        path    : String  = "",
        maxAge  : Int     = -1,
        secure  : Boolean = false,
        comment : String  = "",
        httpOnly: Boolean = false,
        encoding: String  = "UTF-8")

trait HttpCookie {
  implicit def cookieOptions: CookieOptions
  def name: String
  def value: String

}

case class RequestCookie(name: String, value: String, cookieOptions: CookieOptions = CookieOptions()) extends HttpCookie
case class Cookie(name: String, value: String)(implicit val cookieOptions: CookieOptions = CookieOptions()) extends HttpCookie {

  private def ensureDotDomain = if (!cookieOptions.domain.startsWith("."))
    "." + cookieOptions.domain
  else
    cookieOptions.domain

  def toCookieString = {
    val sb = new StringBuffer
    sb append name append "="
    sb append value

    if(cookieOptions.domain.nonBlank)
      sb.append("; Domain=").append(ensureDotDomain.toLowerCase(Locale.ENGLISH))

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


class CookieJar(private val reqCookies: Map[String, RequestCookie]) {
  private lazy val cookies = new ConcurrentHashMap[String, HttpCookie]().asScala ++ reqCookies

  def get(key: String) = cookies.get(key) filter (_.cookieOptions.maxAge != 0) map (_.value)

  def apply(key: String) = get(key) getOrElse (throw new Exception("No cookie could be found for the specified key [%s]" format key))

  def update(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    cookies += name -> Cookie(name, value)(cookieOptions)
  }

  def set(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
    this.update(name, value)(cookieOptions)
  }

  def delete(name: String)(implicit cookieOptions: CookieOptions = CookieOptions(maxAge = 0)) {
    this.update(name, "")(cookieOptions.copy(maxAge = 0))
  }

  def +=(keyValuePair: (String, String))(implicit cookieOptions: CookieOptions = CookieOptions()) = {
    this.update(keyValuePair._1, keyValuePair._2)(cookieOptions)
  }

  def -=(key: String)(implicit cookieOptions: CookieOptions = CookieOptions(maxAge = 0)) {
    delete(key)(cookieOptions)
  }

  private[scalatra] def responseCookies = cookies.values collect { case c: Cookie => c }

}

//object CookieSupport {
//  val SweetCookiesKey = "org.scalatra.SweetCookies"
//  val CookieOptionsKey = "org.scalatra.CookieOptions"
//}
//trait CookieSupport extends Handler {
//  self: ScalatraApp =>
//
//  import CookieSupport._
//  implicit def cookieOptions: CookieOptions = request(CookieOptionsKey).asInstanceOf[CookieOptions]
//
//  def cookies = request(SweetCookiesKey).asInstanceOf[SweetCookies]
//
//  abstract override def handle(req: HttpRequest, res: HttpResponse) {
//    req(SweetCookiesKey) = new SweetCookies(req.cookies, res)
//    val path = contextPath match {
//      case "" => "/" // The root servlet is "", but the root cookie path is "/"
//      case p => p
//    }
//    req(CookieOptionsKey) = CookieOptions(path = path)
//    super.handle(req, res)
//  }
//
//}
