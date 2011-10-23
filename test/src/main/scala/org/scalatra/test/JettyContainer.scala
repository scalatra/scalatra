package org.scalatra.test

import javax.servlet.Filter
import javax.servlet.http.HttpServlet
import java.util.EnumSet
import org.eclipse.jetty.servlet._

object JettyContainer {
  val DefaultDispatcherTypes: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}

trait JettyContainer extends Container {
  import JettyContainer._

  def servletContextHandler: ServletContextHandler

  @deprecated("use addServlet(Class, String) or addFilter(Class, String)")
  def route(klass: Class[_], path: String) = klass match {
    case servlet if classOf[HttpServlet].isAssignableFrom(servlet) =>
      addServlet(servlet.asInstanceOf[Class[_ <: HttpServlet]], path)
    case filter if classOf[Filter].isAssignableFrom(filter) =>
      addFilter(filter.asInstanceOf[Class[_ <: Filter]], path)
    case _ =>
      throw new IllegalArgumentException(klass + " is not assignable to either HttpServlet or Filter")
  }

  @deprecated("renamed to addServlet")
  def route(servlet: HttpServlet, path: String) = addServlet(servlet, path)

  def addServlet(servlet: HttpServlet, path: String) =
    servletContextHandler.addServlet(new ServletHolder(servlet), path)

  def addServlet(servlet: Class[_ <: HttpServlet], path: String) =
    servletContextHandler.addServlet(servlet, path)

  def addFilter(filter: Filter, path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Filter, path: String, dispatches: EnumSet[DispatcherType]): FilterHolder = {
    val holder = new FilterHolder(filter)
    def tryToAddFilter(dispatches: AnyRef) = Reflection.invokeMethod(
      servletContextHandler, "addFilter", holder, path, dispatches)
    // HACK: Jetty7 and Jetty8 have incompatible interfaces.  Call it reflectively
    // so we support both.
    for {
      _ <- tryToAddFilter(DispatcherType.intValue(dispatches): java.lang.Integer).left
      result <- tryToAddFilter(DispatcherType.convert(dispatches, "javax.servlet.DispatcherType")).left
    } yield (throw result)
    holder
  }

  def addFilter(filter: Class[_ <: Filter], path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Class[_ <: Filter], path: String, dispatches: EnumSet[DispatcherType]): FilterHolder = {
    def tryToAddFilter(dispatches: AnyRef): Either[Throwable, AnyRef] =
      Reflection.invokeMethod(servletContextHandler, "addFilter",
        filter, path, dispatches)
    // HACK: Jetty7 and Jetty8 have incompatible interfaces.  Call it reflectively
    // so we support both.
    (tryToAddFilter(DispatcherType.intValue(dispatches): java.lang.Integer).left map {
      t: Throwable => tryToAddFilter(DispatcherType.convert(dispatches, "javax.servlet.DispatcherType"))
    }).joinLeft fold ({ throw _ }, { x => x.asInstanceOf[FilterHolder] })
  }

  @deprecated("renamed to addFilter")
  def routeFilter(filter: Class[_ <: Filter], path: String) =
    addFilter(filter, path)

  // Add a default servlet.  If there is no underlying servlet, then
  // filters just return 404.
  addServlet(classOf[DefaultServlet], "/")

}
