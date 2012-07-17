package org.scalatra.servlet

import java.io.OutputStream
import java.io.PrintWriter
import java.nio.charset.Charset
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import org.scalatra._
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.atomic.AtomicBoolean

class ServletHttpResponse(val r: HttpServletResponse, val cookies: CookieJar) extends HttpResponse {

  private val _ended = new AtomicBoolean(false)
  
  def status: ResponseStatus = ResponseStatus(r.getStatus)
  def status_=(newStatus: ResponseStatus) = {
    r.setStatus(newStatus.code, newStatus.message)
  }

  def characterEncoding_=(cs: String) = r.setCharacterEncoding(cs)

  def characterEncoding: Option[String] = Option(r.getCharacterEncoding)

  // TODO default?
  def serverProtocol: HttpVersion = Http11
  
  def contentType: Option[String] = Option(r.getContentType)
  def contentType_=(ct: String) { r.setContentType(ct) }
  def charset: Charset = Charset.forName("ISO-8859-1")
  def charset_=(cs: Charset) { }
  def chunked: Boolean = false
  def chunked_=(chunked: Boolean) {}

  def outputStream: OutputStream = r.getOutputStream

  def redirect(uri: String) {
    end()
    r.sendRedirect(uri)
  }

  def end() {
    if (_ended.compareAndSet(false, true)) {
      sendCookies
    }
  }
  
  private def sendCookies {
    cookies.responseCookies foreach { cookie => r.addHeader("Set-Cookie", cookie.toCookieString) }
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