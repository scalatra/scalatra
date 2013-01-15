package org.scalatra
package servlet

import java.net.{MalformedURLException, URL}
import java.util.EnumSet
import javax.servlet.{ServletConfig, DispatcherType, Filter, ServletContext}
import javax.servlet.http.{HttpServletResponse, HttpServlet, HttpServletRequest}
import scala.collection.JavaConverters._
import scala.collection.mutable
import java.{ util => jutil }
import org.scalatra.util

/**
 * Extension methods to the standard ServletContext.
 */
case class RichServletContext(sc: ServletContext) extends AttributesMap {
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
   * @param req the request
   * @return the resource located at the result of concatenating the request's
   * servlet path and its path info, or None if there is no resource at that path.
   */
  def resource(req: HttpServletRequest): Option[URL] = {
    val path = req.getServletPath + (Option(req.getPathInfo) getOrElse "")
    resource(path)
  }


  /**
   * Mounts a handler to the servlet context.
   *
   * @param factory the factory to mount
   *
   * @param urlPattern the URL pattern to mount.  Will be appended with `\/\*` if
   * not already, as path-mapping is the most natural fit for Scalatra.
   * If you don't want path mapping, use the native Servlet API.
   *
   * @param name the name of the handler
   */
  def mount[T <: Handler](factory: (ServletConfig, HttpServletRequest, HttpServletResponse) => T, urlPattern: String, name: String) {
    mountServlet(new ScalatraHost(factory), pathMapping(urlPattern), name)
  }

  def mount[T <: Handler : Manifest](factory: (ServletConfig, HttpServletRequest, HttpServletResponse) => T, urlPattern: String): Unit =
    mount(factory, urlPattern, manifest[T].erasure.getName)

  private[this] def pathMapping(urlPattern: String) = urlPattern match {
    case s if s.endsWith("/*") => s
    case s if s.endsWith("/") => s + "*"
    case s => s + "/*"
  }

  /**
   * Mounts a handler to the servlet context.  Must be an HttpServlet or a
   * Filter.
   *
   * @param handler the handler to mount
   * 
   * @param urlPattern the URL pattern to mount.  Will be appended with `\/\*` if
   * not already, as path-mapping is the most natural fit for Scalatra.
   * If you don't want path mapping, use the native Servlet API.
   * 
   * @param name the name of the handler
   */
  def mount(handler: Handler, urlPattern: String, name: String) {
    val pathMap = pathMapping(urlPattern)

    handler match {
      case servlet: HttpServlet => mountServlet(servlet, pathMap, name)
      case filter: Filter => mountFilter(filter, pathMap, name)
      case _ => sys.error("Don't know how to mount this service to a servletContext: " + handler.getClass)
    }
  }

  def mount(handler: Handler, urlPattern: String): Unit =
    mount(handler, urlPattern, handler.getClass.getName)

  def mount[T](handlerClass: Class[T], urlPattern: String, name: String) {
    val pathMap = urlPattern match {
      case s if s.endsWith("/*") => s
      case s if s.endsWith("/") => s + "*"
      case s => s + "/*"
    }

    if (classOf[HttpServlet].isAssignableFrom(handlerClass)) {
      mountServlet(handlerClass.asInstanceOf[Class[HttpServlet]], pathMap, name)
    } else if (classOf[Filter].isAssignableFrom(handlerClass)) {
      mountFilter(handlerClass.asInstanceOf[Class[Filter]], pathMap, name)
    } else {
      sys.error("Don't know how to mount this service to a servletContext: " + handlerClass)
    }
  }

  def mount[T](handlerClass: Class[T], urlPattern: String): Unit =
    mount(handlerClass, urlPattern, handlerClass.getName)

  private def mountServlet(servlet: HttpServlet, urlPattern: String, name: String) {
    val reg = Option(sc.getServletRegistration(name)) getOrElse {
      val r = sc.addServlet(name, servlet)
      servlet match {
        case s: HasMultipartConfig =>
          r.setMultipartConfig(s.multipartConfig.toMultipartConfigElement)
        case _ =>
      }
      if (servlet.isInstanceOf[AsyncSupport])
        r.setAsyncSupported(true)
      r
    }

    reg.addMapping(urlPattern)
  }

  private def mountServlet(servletClass: Class[HttpServlet], urlPattern: String, name: String) {
    val reg = Option(sc.getServletRegistration(name)) getOrElse sc.addServlet(name, servletClass)
    reg.addMapping(urlPattern)
  }

  private def mountFilter(filter: Filter, urlPattern: String, name: String) {
    val reg = Option(sc.getFilterRegistration(name)) getOrElse sc.addFilter(name, filter)
    // We don't have an elegant way of threading this all the way through
    // in an abstract fashion, so we'll dispatch on everything.
    val dispatchers = jutil.EnumSet.allOf(classOf[DispatcherType])
    reg.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }

  private def mountFilter(filterClass: Class[Filter], urlPattern: String, name: String) {
    val reg = Option(sc.getFilterRegistration(name)) getOrElse sc.addFilter(name, filterClass)
    // We don't have an elegant way of threading this all the way through
    // in an abstract fashion, so we'll dispatch on everything.
    val dispatchers = jutil.EnumSet.allOf(classOf[DispatcherType])
    reg.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }

  object initParameters extends mutable.Map[String, String] {
    def get(key: String): Option[String] = Option(sc.getInitParameter(key))

    def iterator: Iterator[(String, String)] = {
      val theInitParams = sc.getInitParameterNames

      new Iterator[(String, String)] {

        def hasNext: Boolean = theInitParams.hasMoreElements

        def next(): (String, String) = {
          val nm = theInitParams.nextElement()
          (nm, sc.getInitParameter(nm))
        }
      }
    }

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

