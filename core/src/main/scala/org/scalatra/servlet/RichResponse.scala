package org.scalatra
package servlet

import java.io.{ OutputStream, PrintWriter }
import jakarta.servlet.http.{ HttpServletResponse, Cookie => ServletCookie }

import org.scalatra.util.RicherString._

case class RichResponse(res: HttpServletResponse) {

  /**
   * Note: the servlet API doesn't remember the reason.  If a custom
   * reason was set, it will be returned incorrectly here,
   */
  def status: Int = res.getStatus

  def status_=(status: Int): Unit = {
    res.setStatus(status)
  }

  object headers {
    def update(name: String, value: String): Unit = {
      res.setHeader(name, value)
    }

    def set(name: String, value: String): Unit = update(name, value)
  }

  def addCookie(cookie: Cookie): Unit = {
    import cookie._

    val sCookie = new ServletCookie(name, value)
    if (options.domain.nonBlank) sCookie.setDomain(options.domain)
    if (options.path.nonBlank) sCookie.setPath(options.path)
    sCookie.setMaxAge(options.maxAge)
    sCookie.setSecure(options.secure)
    if (options.comment.nonBlank) sCookie.setComment(options.comment)
    sCookie.setHttpOnly(options.httpOnly)
    sCookie.setVersion(options.version)
    res.addCookie(sCookie)
  }

  def characterEncoding: Option[String] = Option(res.getCharacterEncoding)

  def characterEncoding_=(encoding: Option[String]): Unit = {
    res.setCharacterEncoding(encoding.orNull)
  }

  def contentType: Option[String] = Option(res.getContentType)

  def contentType_=(contentType: Option[String]): Unit = {
    res.setContentType(contentType.orNull)
  }

  def redirect(uri: String): Unit = {
    res.sendRedirect(uri)
  }

  def outputStream: OutputStream = res.getOutputStream

  def writer: PrintWriter = res.getWriter

  def end(): Unit = {
    res.flushBuffer()
    res.getOutputStream.close()
  }

}
