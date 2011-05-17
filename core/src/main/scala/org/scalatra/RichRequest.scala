package org.scalatra

import scala.collection.{Map => CMap}
import scala.io.Source
import javax.servlet.http.HttpServletRequest
import java.util.Locale
import util.{MultiMap, MultiMapHeadView}

case class RichRequest(r: HttpServletRequest) extends AttributesMap {
  @deprecated(message = "Use HttpServletRequest.getServerName() instead")
  def host = r.getServerName

  @deprecated(message = "Use HttpServletRequest.getServerPort() instead")
  def port = Integer.toString(r.getServerPort)

  def referer = r.getHeader("Referer") match {
    case s: String => Some(s)
    case null => None
  }

  def body:String = {
    val encoding = r.getCharacterEncoding
    val enc = if(encoding == null || encoding.trim.length == 0) {
      "ISO-8859-1"
    } else encoding
    Source.fromInputStream(r.getInputStream, enc).mkString
  }

  def isAjax: Boolean = r.getHeader("X-Requested-With") != null
  def isWrite: Boolean = !HttpMethod(r.getMethod).isSafe

  def multiCookies: MultiMap = {
    val rr = Option(r.getCookies).getOrElse(Array()).toSeq.
      groupBy { _.getName }.
      transform { case(k, v) => v map { _.getValue }}.
      withDefaultValue(Seq.empty)
    MultiMap(rr)
  }

  def cookies: CMap[String, String] = new MultiMapHeadView[String, String] { protected def multiMap = multiCookies }

  /*
   * TODO The structural type works at runtime, but fails to compile because
   * of the raw type returned by getAttributeNames.  We're telling the
   * compiler to trust us; remove when we upgrade to Servlet 3.0.
   */
  protected def attributes = r.asInstanceOf[Attributes]
}

