package org.scalatra
package servlet

import util.RicherString._

import java.io.{OutputStream, PrintWriter}
import javax.servlet.http.{HttpServletResponse, HttpServletResponseWrapper, Cookie => ServletCookie}
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

object ServletResponse {
  def apply(response: HttpServletResponse) = new ServletResponse(response)
}

class ServletResponse(res: HttpServletResponse) 
  extends HttpServletResponseWrapper(res)
  with Response 
{
  /**
   * Note: the servlet API doesn't remember the reason.  If a custom
   * reason was set, it will be returned incorrectly here,
   */
  def status: ResponseStatus = ResponseStatus(res.getStatus)

  def status_=(statusLine: ResponseStatus) = 
    res.setStatus(statusLine.code, statusLine.message)

  object headers extends Map[String, String] {
    def get(key: String): Option[String] = 
      res.getHeaders(key) match {
	case xs if xs.isEmpty => None
	case xs => Some(xs mkString ",")
      }

    def iterator: Iterator[(String, String)] = 
      for (name <- res.getHeaderNames.iterator) 
      yield (name, res.getHeaders(name) mkString ", ")

    def +=(kv: (String, String)): this.type = {
      res.setHeader(kv._1, kv._2)
      this
    }

    def -=(key: String): this.type = {
      res.setHeader(key, "")
      this
    }
  }

  def addCookie(cookie: Cookie) {
    import cookie._

    val sCookie = new ServletCookie(name, value)
    if (options.domain.nonBlank) sCookie.setDomain(options.domain)
    if(options.path.nonBlank) sCookie.setPath(options.path)
    sCookie.setMaxAge(options.maxAge)
    if(options.secure) sCookie.setSecure(options.secure)
    if(options.comment.nonBlank) sCookie.setComment(options.comment)
    sCookie.setHttpOnly(options.httpOnly)
    addCookie(sCookie)
  }

  def characterEncoding: Option[String] =
    Option(res.getCharacterEncoding)

  def characterEncoding_=(encoding: Option[String]) {
    res.setCharacterEncoding(encoding getOrElse null)
  }
  
  def contentType: Option[String] =
    Option(res.getContentType)

  def contentType_=(contentType: Option[String]) {
    res.setContentType(contentType getOrElse null) 
  }
  
  def redirect(uri: String) {
    res.sendRedirect(uri)
  }
  
  def outputStream: OutputStream = 
    res.getOutputStream

  def writer: PrintWriter =
    res.getWriter

  def end() = {
    res.flushBuffer()
    res.getOutputStream.close()
  }

  override def addHeader(name: String, value: String) = 
    super.addHeader(name, value)
}
