package org.scalatra

import java.net.URLEncoder.encode
import javax.servlet.http.HttpServletResponse

/**
 * Provides utility methods for the creation of URL strings.
 * Supports context-relative and session-aware URLs.  Should behave similarly to JSTL's <c:url> tag.
 */
trait UrlSupport {
  protected def contextPath: String

  protected def response: HttpServletResponse

  /**
   * Returns a context-relative, session-aware URL for a path with no
   * query parameters.
   *
   * @see url(String, Iterable[(String, Any])
   */
  def url(path: String): String = url(path, Seq.empty)

  /**
   * Returns a context-relative, session-aware URL for a path and specified
   * parameters.
   * Finally, the result is run through `response.encodeURL` for a session
   * ID, if necessary.
   *
   * @param path the base path.  If a path begins with '/', then the context
   * path will be prepended to the result
   *
   * @param params params, to be appended in the form of a query string
   *
   * @return the path plus the query string, if any.  The path is run through
   * `response.encodeURL` to add any necessary session tracking parameters.
   */
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
