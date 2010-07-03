package org.scalatra

import java.net.URLEncoder.encode
import javax.servlet.http.HttpServletResponse

/*
 * Supports context-relative and session-aware URLs.  Should behave similarly to JSTL's <c:url> tag.
 */
trait UrlSupport {
  protected def contextPath: String

  protected def response: HttpServletResponse

  def url(path: String): String = url(path, Seq.empty)

  def url(path: String, params: Iterable[(String, Any)]): String = {
    val newPath = path match {
      case x if x.startsWith("/") => contextPath + path
      case _ => path
    }
    val pairs = params map { case(key, value) => encode(key, "utf-8") + "=" +encode(value.toString, "utf-8") }
    val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
    response.encodeURL(newPath+queryString)
  }
}