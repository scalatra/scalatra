package org.scalatra
package servlet

import java.net.{MalformedURLException, URL}
import java.util.EnumSet
import javax.servlet.{DispatcherType, Filter, ServletContext}
import javax.servlet.http.{HttpServlet, HttpServletRequest}
import scala.collection.JavaConversions._
import scala.collection.mutable

object ServletApplicationContext {
  def apply(servletContext: ServletContext) = 
    new ServletApplicationContext(servletContext)
}

/**
 * Extension methods to the standard ServletContext.
 */
class ServletApplicationContext(sc: ServletContext) 
  extends ServletContextWrapper(sc)
  with ApplicationContext 
  with AttributesMap 
{
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

  /**
   * Mounts a handler to the servlet context.  Must be an HttpServlet or a
   * Filter.
   *
   * @handler the handler to mount
   * 
   * @urlPattern the URL pattern to mount.  Will be appended with `\/\*` if
   * not already, as path-mapping is the most natural fit for Scalatra.
   * If you don't want path mapping, use the native Servlet API.
   * 
   * @name the name of the handler
   */
  def mount(handler: Handler, urlPattern: String, name: String) {
    val pathMap = urlPattern match {
      case s if s.endsWith("/*") => s
      case s if s.endsWith("/") => s + "*"
      case s => s + "/*"
    }

    handler match {
      case servlet: HttpServlet => mountServlet(servlet, pathMap, name)
      case filter: Filter => mountFilter(filter, pathMap, name)
      case _ => sys.error("Don't know how to mount this service to a servletContext: " + handler.getClass)
    }
  }

  private def mountServlet(servlet: HttpServlet, urlPattern: String, name: String) {
    val reg = sc.addServlet(name, servlet)
    reg.addMapping(urlPattern)
  }

  private def mountFilter(filter: Filter, urlPattern: String, name: String) {
    val reg = sc.addFilter(name, filter)
    // We don't have an elegant way of threading this all the way through
    // in an abstract fashion, so we'll dispatch on everything.
    val dispatchers = EnumSet.allOf(classOf[DispatcherType])
    reg.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }

  object initParameters extends mutable.Map[String, String] {
    def get(key: String): Option[String] = Option(sc.getInitParameter(key))

    def iterator: Iterator[(String, String)] = 
      for (name <- sc.getInitParameterNames.toIterator) 
      yield (name, sc.getInitParameter(name))

    def +=(kv: (String, String)): this.type = {
      sc.setInitParameter(kv._1, kv._2)
      this
    }

    def -=(key: String): this.type = {
      sc.setInitParameter(key, null)
      this
    }
  }

  def contextPath = sc.getContextPath
}

