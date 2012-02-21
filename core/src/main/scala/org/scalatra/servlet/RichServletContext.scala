package org.scalatra
package servlet

import java.net.{MalformedURLException, URL}
import java.util.EnumSet
import javax.servlet.{DispatcherType, Filter, ServletContext}
import javax.servlet.http.{HttpServlet, HttpServletRequest}

/**
 * Extension methods to the standard ServletContext.
 */
class RichServletContext(sc: ServletContext) extends AttributesMap {
  protected def attributes = sc

  /**
   * Optionally returns a URL to the resource mapped to the given path.  This
   * is a wrapper around `getResource`.
   *
   * @param path the path to the resource
   * @return the resource located at the path, or None if there is no resource
   * at that path.
   */
  def resource(path: String): Option[URL] =
    try {
      Option(sc.getResource(path))
    }
    catch {
      case e: MalformedURLException => throw e
    }

  /**
   * Optionally returns the resource mapped to the request's path.
   *
   * @param the request
   * @return the resource located at the result of concatenating the request's
   * servlet path and its path info, or None if there is no resource at that path.
   */
  def resource(req: HttpServletRequest): Option[URL] = {
    val path = req.getServletPath + (Option(req.getPathInfo) getOrElse "")
    resource(path)
  }

  def mount(servlet: HttpServlet, urlPattern: String) {
    val reg = sc.addServlet(servlet.getClass.getName, servlet)
    reg.addMapping(urlPattern)
  }

  def mount(filter: Filter, urlPattern: String)(implicit dispatchers: EnumSet[DispatcherType]) {
    val reg = sc.addFilter(filter.getClass.getName, filter)
    reg.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }
}

