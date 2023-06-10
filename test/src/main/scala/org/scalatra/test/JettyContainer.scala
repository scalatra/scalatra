package org.scalatra
package test

import java.util
import java.util.EnumSet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.{ DispatcherType, Filter }

import org.eclipse.jetty.servlet._
import org.scalatra.servlet.{ HasMultipartConfig, ScalatraAsyncSupport }

object JettyContainer {
  private val DefaultDispatcherTypes: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}

trait JettyContainer extends Container {
  import org.scalatra.test.JettyContainer._

  def servletContextHandler: ServletContextHandler
  def skipDefaultServlet: Boolean = false

  def mount(klass: Class[_], path: String) = klass match {
    case servlet if classOf[HttpServlet].isAssignableFrom(servlet) =>
      addServlet(servlet.asInstanceOf[Class[_ <: HttpServlet]], path)
    case filter if classOf[Filter].isAssignableFrom(filter) =>
      addFilter(filter.asInstanceOf[Class[_ <: Filter]], path)
    case _ =>
      throw new IllegalArgumentException(klass.toString + " is not assignable to either HttpServlet or Filter")
  }

  def mount(servlet: HttpServlet, path: String): Unit = { addServlet(servlet, path) }
  def mount(servlet: HttpServlet, path: String, name: String): Unit = { addServlet(servlet, path, name) }

  def mount(app: Filter, path: String, dispatches: EnumSet[DispatcherType] = DefaultDispatcherTypes) =
    addFilter(app, path, dispatches)

  def addServlet(servlet: HttpServlet, path: String): Unit = { addServlet(servlet, path, servlet.getClass.getName) }
  def addServlet(servlet: HttpServlet, path: String, name: String): Unit = {
    val holder = new ServletHolder(name, servlet)

    servlet match {
      case s: HasMultipartConfig => {
        holder.getRegistration.setMultipartConfig(
          s.multipartConfig.toMultipartConfigElement)
      }
      case s: ScalatraAsyncSupport =>
        holder.getRegistration.setAsyncSupported(true)
      case _ =>
    }

    servletContextHandler.addServlet(holder, if (path.endsWith("/*")) path else path + "/*")

  }

  def addServlet(servlet: Class[_ <: HttpServlet], path: String) =
    servletContextHandler.addServlet(servlet, path)

  def addFilter(filter: Filter, path: String, dispatches: util.EnumSet[DispatcherType] = DefaultDispatcherTypes): FilterHolder = {
    val holder = new FilterHolder(filter)
    servletContextHandler.addFilter(holder, path, dispatches)
    holder
  }

  def addFilter(filter: Class[_ <: Filter], path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Class[_ <: Filter], path: String, dispatches: util.EnumSet[DispatcherType]): FilterHolder =
    servletContextHandler.addFilter(filter, path, dispatches)

  // Add a default servlet.  If there is no underlying servlet, then
  // filters just return 404.
  if (!skipDefaultServlet) servletContextHandler.addServlet(new ServletHolder("default", classOf[DefaultServlet]), "/")

  protected def ensureSessionIsSerializable(): Unit = {
    servletContextHandler.getSessionHandler.addEventListener(SessionSerializingListener)
  }
}
