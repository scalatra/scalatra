package org.scalatra
package servlet

import java.io.{OutputStream, PrintWriter}
import javax.servlet.http.HttpServletResponse

case class RichResponse(res: HttpServletResponse) extends HttpResponse {
  def status: Int = { res.getStatus }
    
  def status_=(status: Int) { 
    res.setStatus(status)
  }
  
  def status_=(status: (Int, String)) { 
    res.setStatus(status._1, status._2)
  }
  
  def header(name: String) =
    Option(res.getHeader(name))

  def addHeader(name: String, value: String) {
    res.addHeader(name, value)
  }
  def setHeader(name: String, value: String) {
    res.setHeader(name, value)
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
}
