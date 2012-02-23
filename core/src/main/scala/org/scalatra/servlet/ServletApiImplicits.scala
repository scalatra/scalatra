package org.scalatra
package servlet

import java.io.{OutputStream, PrintWriter}
import java.net.URI
import java.util.EnumSet
import javax.servlet.DispatcherType
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import http.{HttpRequest, HttpResponse}
import scala.collection.JavaConversions._

/**
 * Some implicits to make the Servlet API more Scala-idiomatic.
 */
trait ServletApiImplicits {
  implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  implicit def sessionWrapper(s: HttpSession) = new RichSession(s)
  implicit def servletContextWrapper(sc: ServletContext) = new RichServletContext(sc)
  implicit def DefaultDispatchers: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)

  implicit object ServletHttpRequest extends HttpRequest[HttpServletRequest] {
    val default = null

    def uri(implicit req: HttpServletRequest) = new URI(req.getRequestURL.toString)

    def isSecure(implicit req: HttpServletRequest) = req.isSecure 

    def method(implicit req: HttpServletRequest) = req.method
    def parameters(implicit req: HttpServletRequest): ScalatraKernel.MultiParams = {
      req.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]].toMap
        .transform { (k, v) => v: Seq[String] }
    }

    def characterEncoding(implicit req: HttpServletRequest): String =
      req.getCharacterEncoding

    def characterEncoding_=(encoding: String)(implicit req: HttpServletRequest) {
      req.setCharacterEncoding(encoding)
    }
    
    def contentType(implicit req: HttpServletRequest): String =
      req.getContentType
    
    def apply(key: String)(implicit req: HttpServletRequest) =
      req.getAttribute(key)

    def update(key: String, value: Any)(implicit req: HttpServletRequest) {
      req.setAttribute(key, value)
    }
  }

  implicit object ServletHttpResponse extends HttpResponse[HttpServletResponse] {
    val default = null

    def status(implicit res: HttpServletResponse): Int = { res.getStatus }
    
    def status_=(status: Int)(implicit res: HttpServletResponse) { 
      res.setStatus(status) 
    }
    
    def status_=(status: (Int, String))(implicit res: HttpServletResponse) { 
      res.setStatus(status._1, status._2)
    }
    
    def addHeader(name: String, value: String)(implicit res: HttpServletResponse) {
      res.addHeader(name, value)
    }
    def setHeader(name: String, value: String)(implicit res: HttpServletResponse) {
      res.setHeader(name, value)
    }

    def characterEncoding(implicit res: HttpServletResponse): String =
      res.getCharacterEncoding

    def characterEncoding_=(encoding: String)(implicit res: HttpServletResponse) {
      res.setCharacterEncoding(encoding)
    }
    
    def contentType(implicit res: HttpServletResponse): String =
      res.getContentType

    def contentType_=(contentType: String)(implicit res: HttpServletResponse) {
      res.setContentType(contentType)
    }
    
    def redirect(uri: String)(implicit res: HttpServletResponse) {
      res.sendRedirect(uri)
    }
    
    def outputStream(implicit res: HttpServletResponse): OutputStream = 
      res.getOutputStream

    def writer(implicit res: HttpServletResponse): PrintWriter =
      res.getWriter
  }
}
