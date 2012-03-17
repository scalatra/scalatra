package org.scalatra.test

import javax.servlet.{DispatcherType, Filter}
import javax.servlet.http.HttpServlet
import java.util.EnumSet
import org.eclipse.jetty.servlet._

object JettyContainer {
  private val DefaultDispatcherTypes: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}

trait JettyContainer extends Container {
  import JettyContainer._

  def servletContextHandler: ServletContextHandler

  @deprecated("use addServlet(Class, String) or addFilter(Class, String)", "2.0.0")
  def route(klass: Class[_], path: String) = klass match {
    case servlet if classOf[HttpServlet].isAssignableFrom(servlet) =>
      addServlet(servlet.asInstanceOf[Class[_ <: HttpServlet]], path)
    case filter if classOf[Filter].isAssignableFrom(filter) =>
      addFilter(filter.asInstanceOf[Class[_ <: Filter]], path)
    case _ =>
      throw new IllegalArgumentException(klass + " is not assignable to either HttpServlet or Filter")
  }

  @deprecated("renamed to addServlet", "2.0.0")
  def route(servlet: HttpServlet, path: String) = addServlet(servlet, path)

  def addServlet(servlet: HttpServlet, path: String) =
    servletContextHandler.addServlet(new ServletHolder(servlet), path)

  def addServlet(servlet: Class[_ <: HttpServlet], path: String) =
    servletContextHandler.addServlet(servlet, path)

  def addFilter(filter: Filter, path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Filter, path: String, dispatches: EnumSet[DispatcherType]): FilterHolder = {
    val holder = new FilterHolder(filter)
    servletContextHandler.addFilter(holder, path, dispatches)
    holder
  }

  def addFilter(filter: Class[_ <: Filter], path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Class[_ <: Filter], path: String, dispatches: EnumSet[DispatcherType]): FilterHolder =
    servletContextHandler.addFilter(filter, path, dispatches)

  @deprecated("renamed to addFilter", "2.0.0")
  def routeFilter(filter: Class[_ <: Filter], path: String) =
    addFilter(filter, path)

  // Add a default servlet.  If there is no underlying servlet, then
  // filters just return 404.
  addServlet(classOf[DefaultServlet], "/")

}
