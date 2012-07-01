package org.scalatra.servlet

import java.io.OutputStream
import java.io.PrintWriter
import java.nio.charset.Charset

import scala.collection.JavaConversions._
import scala.collection.mutable.Map

import org.scalatra._

import javax.servlet.http.HttpServletResponse

class ServletHttpResponse(val r: HttpServletResponse, val cookies: CookieJar) extends HttpResponse {

  def status: ResponseStatus = ResponseStatus(r.getStatus)
  def status_=(newStatus: ResponseStatus) = r.setStatus(newStatus.code)

  def characterEncoding_=(cs: String) = r.setCharacterEncoding(cs)

  def characterEncoding: Option[String] = Option(r.getCharacterEncoding)

  def contentType: Option[String] = Option(r.getContentType)
  def contentType_=(ct: String) { r.setContentType(ct) }
  def charset: Charset = Charset.forName("ISO-8859-1")
  def charset_=(cs: Charset) { }
  def chunked: Boolean = false
  def chunked_=(chunked: Boolean) {}

  def outputStream: OutputStream = r.getOutputStream

  // TODO cache?
  def writer: PrintWriter = new PrintWriter(outputStream)

  def redirect(uri: String) {
    r.sendRedirect(uri)
  }

  def end() {
    cookies.responseCookies foreach { cookie => 
      r.addHeader("Set-Cookie", cookie.toCookieString)
    }
  }

  def addCookie(cookie: Cookie) {
    cookies.update(cookie.name, cookie.value)(cookie.cookieOptions)
  }

  object headers extends Map[String, String] {
    def get(key: String): Option[String] = 
      r.getHeaders(key) match {
        case xs if xs.isEmpty => None
        case xs => Some(xs mkString ",")
      }

    def iterator: Iterator[(String, String)] = 
      for (name <- r.getHeaderNames.iterator) 
      yield (name, r.getHeaders(name) mkString ", ")

    def +=(kv: (String, String)): this.type = {
      r.setHeader(kv._1, kv._2)
      this
    }

    def -=(key: String): this.type = {
      r.setHeader(key, "")
      this
    }
  }
 
}