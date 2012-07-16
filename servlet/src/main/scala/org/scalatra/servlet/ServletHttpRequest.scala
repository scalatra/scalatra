package org.scalatra.servlet

import java.io.InputStream
import java.net.URI

import scala.collection.GenSeq
import scala.collection.JavaConversions._
import scala.io.Source
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse, HttpServletResponseWrapper, Cookie => ServletCookie}

import org.scalatra._
import org.scalatra.util.io.PathManipulationOps

class ServletHttpRequest(val r: HttpServletRequest) extends HttpRequest {
  
  def requestMethod = _method 

  private var _method:HttpMethod = r.getMethod match {
    case method:String => HttpMethod(method)
    case x => ExtensionMethod(x)
  }
  
  def requestMethod_=(method: HttpMethod) {
    _method = method
  } 

  def scriptName = r.getContextPath

  def pathInfo = PathManipulationOps.ensureSlash(r.getRequestURI.replaceFirst("^" + scriptName, ""))

  def queryString = Option(r.getQueryString) getOrElse ""
   
  def serverName = r.getServerName

  def serverPort = r.getServerPort

  def urlScheme = r.getScheme match {
    case "http" => Http
    case "https" => Https
  }

  def contentLength: Option[Long] = r.getContentLength match {
    case -1 => None
    case length => Some(length)
  }

  def inputStream: InputStream = r.getInputStream

  def uri = new URI(r.getRequestURL.toString)

  def isSecure: Boolean = urlScheme match {
    case Https => true
    case _ => false
  }

  def serverProtocol = r.getProtocol match {
    case "HTTP/1.1" => Http11
    case "HTTP/1.0" => Http10
  }

  /**
   * A Map of the parameters of this request. Parameters are contained in
   * the query string or posted form data.
   */
  def multiParameters: MultiParams = r.getParameterNames.map(key => (key -> r.getParameterValues(key).toSeq)).toMap
  attributes(MultiParamsKey) = multiParameters

  val cookies = {
    val servletCookies = r.getCookies
    val requestCookies =
      Map((servletCookies map { jc =>
        val reqCookie: RequestCookie = jc
        reqCookie.name -> reqCookie
      }).toList:_*)
    new CookieJar(requestCookies)
  }

  // TODO
  def files: GenSeq[HttpFile] = Seq()

  def body:String = {
    cachedBody getOrElse {
      val encoding = r.getCharacterEncoding
      val enc = if(encoding == null || encoding.trim.length == 0) {
        "ISO-8859-1"
      } else encoding
      val body = Source.fromInputStream(r.getInputStream, enc).mkString
      update(cachedBodyKey, body)
      body
    }
  }

  private val cachedBodyKey = "org.scalatra.servlet.cachedBody"
  private def cachedBody: Option[String] = get(cachedBodyKey).asInstanceOf[Option[String]]

  /**
   * TODO
   * The remote address the client is connected from.
   * This takes the load balancing header X-Forwarded-For into account
   * @return the client ip address
   */
  def remoteAddress: String = r.getRemoteAddr
  
  def headers = r.getHeaderNames.map{ name => (name -> r.getHeader(name)) }.toMap

  def contentType: Option[String] = Option(r.getContentType)

  def characterEncoding: Option[String] = Option(r.getCharacterEncoding)

}