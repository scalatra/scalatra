package org.scalatra
package servlet

import java.net.{ MalformedURLException, URL }
import java.{ util => jutil }
import javax.servlet.http.{ HttpServlet, HttpServletRequest }
import javax.servlet.{ DispatcherType, Filter, ServletContext }

import scala.collection.mutable

/**
 * Extension methods to the standard ServletContext.
 */
case class RichServletContext(sc: ServletContext) extends AttributesMap {

  protected def attributes: ServletContext = sc

  /**
   * Optionally returns a URL to the resource mapped to the given path.  This
   * is a wrapper around `getResource`.
   *
   * @param path the path to the resource
   * @return the resource located at the path, or None if there is no resource
   * at that path.
   */
  def resource(path: String): Option[URL] = {
    try {
      Option(sc.getResource(path))
    } catch {
      case e: MalformedURLException => throw e
    }
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

  private[this] def pathMapping(urlPattern: String): String = urlPattern match {
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
  def mount(handler: Handler, urlPattern: String, name: String): Unit = {
    mount(handler, urlPattern, name, 1)
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
  def mount(handler: Handler, urlPattern: String, name: String, loadOnStartup: Int): Unit = {
    val pathMap = pathMapping(urlPattern)

    handler match {
      case servlet: HttpServlet => mountServlet(servlet, pathMap, name, loadOnStartup)
      case filter: Filter => mountFilter(filter, pathMap, name)
      case _ => sys.error("Don't know how to mount this service to a servletContext: " + handler.getClass)
    }
  }

  def mount(handler: Handler, urlPattern: String): Unit = mount(handler, urlPattern, 1)

  def mount(handler: Handler, urlPattern: String, loadOnStartup: Int): Unit = {
    mount(handler, urlPattern, handler.getClass.getName, loadOnStartup)
  }

  def mount[T](
    handlerClass: Class[T],
    urlPattern: String,
    name: String,
    loadOnStartup: Int = 1): Unit = {
    val pathMap = urlPattern match {
      case s if s.endsWith("/*") => s
      case s if s.endsWith("/") => s + "*"
      case s => s + "/*"
    }

    if (classOf[HttpServlet].isAssignableFrom(handlerClass)) {
      mountServlet(handlerClass.asInstanceOf[Class[HttpServlet]], pathMap, name, loadOnStartup)
    } else if (classOf[Filter].isAssignableFrom(handlerClass)) {
      mountFilter(handlerClass.asInstanceOf[Class[Filter]], pathMap, name)
    } else {
      sys.error("Don't know how to mount this service to a servletContext: " + handlerClass)
    }
  }

  def mount[T](handlerClass: Class[T], urlPattern: String): Unit = mount[T](handlerClass, urlPattern, 1)

  def mount[T](handlerClass: Class[T], urlPattern: String, loadOnStartup: Int): Unit = {
    mount(handlerClass, urlPattern, handlerClass.getName, loadOnStartup)
  }

  private def mountServlet(
    servlet: HttpServlet,
    urlPattern: String,
    name: String,
    loadOnStartup: Int): Unit = {
    val reg = Option(sc.getServletRegistration(name)) getOrElse {
      val r = sc.addServlet(name, servlet)
      servlet match {
        case s: HasMultipartConfig =>
          r.setMultipartConfig(s.multipartConfig.toMultipartConfigElement)
        case _ =>
      }
      if (servlet.isInstanceOf[ScalatraAsyncSupport])
        r.setAsyncSupported(true)
      r.setLoadOnStartup(loadOnStartup)
      r
    }

    reg.addMapping(urlPattern)
  }

  private def mountServlet(
    servletClass: Class[HttpServlet],
    urlPattern: String,
    name: String,
    loadOnStartup: Int): Unit = {
    val reg = Option(sc.getServletRegistration(name)) getOrElse {
      val r = sc.addServlet(name, servletClass)
      // since we only have a Class[_] here, we can't access the MultipartConfig value
      // if (classOf[HasMultipartConfig].isAssignableFrom(servletClass))
      if (classOf[ScalatraAsyncSupport].isAssignableFrom(servletClass)) {
        r.setAsyncSupported(true)
      }
      r.setLoadOnStartup(loadOnStartup)
      r
    }
    reg.addMapping(urlPattern)
  }

  private def mountFilter(filter: Filter, urlPattern: String, name: String): Unit = {
    val reg = Option(sc.getFilterRegistration(name)) getOrElse {
      val r = sc.addFilter(name, filter)
      if (filter.isInstanceOf[ScalatraAsyncSupport])
        r.setAsyncSupported(true)
      r
    }
    // We don't have an elegant way of threading this all the way through
    // in an abstract fashion, so we'll dispatch on everything.
    val dispatchers = jutil.EnumSet.allOf(classOf[DispatcherType])
    reg.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }

  private def mountFilter(filterClass: Class[Filter], urlPattern: String, name: String): Unit = {
    val reg = Option(sc.getFilterRegistration(name)) getOrElse {
      val r = sc.addFilter(name, filterClass)
      if (classOf[ScalatraAsyncSupport].isAssignableFrom(filterClass)) {
        r.setAsyncSupported(true)
      }
      r
    }
    // We don't have an elegant way of threading this all the way through
    // in an abstract fashion, so we'll dispatch on everything.
    val dispatchers = jutil.EnumSet.allOf(classOf[DispatcherType])
    reg.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }

  /**
   * A free form string representing the environment.
   * `org.scalatra.Environment` is looked up as a system property, and if
   * absent, as an init parameter.  The default value is `DEVELOPMENT`.
   */
  def environment: String = {
    sys.props.get(EnvironmentKey) orElse initParameters.get(EnvironmentKey) getOrElse ("DEVELOPMENT")
  }

  object initParameters extends mutable.Map[String, String] {

    def get(key: String): Option[String] = Option(sc.getInitParameter(key))

    def iterator: Iterator[(String, String)] = {
      val theInitParams = sc.getInitParameterNames
      new Iterator[(String, String)] {
        override def hasNext: Boolean = theInitParams.hasMoreElements
        override def next(): (String, String) = {
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

  def contextPath: String = sc.getContextPath

}

