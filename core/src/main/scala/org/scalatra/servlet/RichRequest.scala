package org.scalatra
package servlet

import scala.collection.{Map => CMap}
import scala.collection.JavaConversions._
import scala.io.Source
import java.net.URI
import javax.servlet.http.HttpServletRequest
import http.HttpRequest
import util.{MultiMap, MultiMapHeadView}

object RichRequest {
  private val cachedBodyKey = "org.scalatra.RichRequest.cachedBody"
}

/**
 * Extension methods to a standard HttpServletRequest.
 */
case class RichRequest(r: HttpServletRequest) extends HttpRequest with AttributesMap {
  import RichRequest._

  def uri = new URI(r.getRequestURL.toString)

  def isSecure = r.isSecure 

  /**
   * Returns the request's method.
   */
  def method = HttpMethod(r.getMethod)

  def parameters: ScalatraKernel.MultiParams = {
    r.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]].toMap
      .transform { (k, v) => v: Seq[String] }
  }
  
  def header(name: String): Option[String] =
    Option(r.getHeader(name))
  
  def characterEncoding: Option[String] =
    Option(r.getCharacterEncoding)
  
  def characterEncoding_=(encoding: Option[String]) {
    r.setCharacterEncoding(encoding getOrElse null)
  }
  
  def contentType: Option[String] =
    Option(r.getContentType)
  
  @deprecated(message = "Use HttpServletRequest.getServerName() instead")
  def host = r.getServerName

  @deprecated(message = "Use HttpServletRequest.getServerPort() instead")
  def port = Integer.toString(r.getServerPort)

  /**
   * Optionally returns the HTTP referrer.
   *
   * @return the `Referer` header, or None if not set
   */
  def referrer: Option[String] = r.getHeader("Referer") match {
    case s: String => Some(s)
    case null => None
  }

  @deprecated("Use referrer")
  def referer: Option[String] = referrer

  /**
   * Caches and returns the body of the response.  The method is idempotent
   * for any given request.  The result is cached in memory regardless of size,
   * so be careful.  Calling this method consumes the request's input stream.
   *
   * @return the message body as a string according to the request's encoding
   * (defult ISO-8859-1).
   */
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

  private def cachedBody: Option[String] =
    get(cachedBodyKey).asInstanceOf[Option[String]]

  /**
   * Returns true if the request is an AJAX request
   */
  def isAjax: Boolean = r.getHeader("X-Requested-With") != null

  /**
   * Returns true if the request's method is not "safe" per RFC 2616.
   */
  def isWrite: Boolean = !HttpMethod(r.getMethod).isSafe

  /**
   * Returns a map of cookie names to lists of their values.  The default
   * value of the map is the empty seuqence.
   */
  def multiCookies: MultiMap = {
    val rr = Option(r.getCookies).getOrElse(Array()).toSeq.
      groupBy { _.getName }.
      transform { case(k, v) => v map { _.getValue }}.
      withDefaultValue(Seq.empty)
    MultiMap(rr)
  }

  /**
   * Returns a map of cookie names to values.  If multiple values are present
   * for a given cookie, the value is the first cookie of that name.
   */
  def cookies: CMap[String, String] = new MultiMapHeadView[String, String] { protected def multiMap = multiCookies }

  protected def attributes = r
}
