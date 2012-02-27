package org.scalatra
package servlet

import java.net.{MalformedURLException, URL}
import java.util.EnumSet
import javax.servlet.{DispatcherType, Filter, ServletContext}
import javax.servlet.http.{HttpServlet, HttpServletRequest}
import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Extension methods to the standard ServletContext.
 */
class RichServletContext(sc: ServletContext) extends ApplicationContext with AttributesMap {
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

  def mount(service: Service, urlPattern: String) {
    service match {
      case servlet: HttpServlet => mountServlet(servlet, urlPattern)
      case filter: Filter => mountFilter(filter, urlPattern)
      case _ => error("Don't know how to mount this service to a servletContext: " + service.getClass)
    }
  }

  private def mountServlet(servlet: HttpServlet, urlPattern: String) {
    val reg = sc.addServlet(servlet.getClass.getName, servlet)
    reg.addMapping(urlPattern)
  }

  private def mountFilter(filter: Filter, urlPattern: String) {
    val reg = sc.addFilter(filter.getClass.getName, filter)
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

