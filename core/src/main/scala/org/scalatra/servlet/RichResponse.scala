package org.scalatra
package servlet

import java.io.{OutputStream, PrintWriter}
import javax.servlet.http.HttpServletResponse
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

case class RichResponse(res: HttpServletResponse) extends HttpResponse {
  def status: ResponseStatus = ResponseStatus(res.getStatus)
    
  def status_=(status: ResponseStatus) { 
    res.setStatus(status.code, status.message)
  }

  object headers extends Map[String, String] {
    def get(key: String): Option[String] = 
      res.getHeaders(key) match {
	case xs if xs.isEmpty => None
	case xs => Some(xs mkString ", ")
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

  def end() = {
    res.flushBuffer()
    res.getOutputStream.close()
  }
}
