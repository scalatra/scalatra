package org.scalatra
package jetty

import java.util.EnumSet
import javax.servlet.{DispatcherType, Filter}
import javax.servlet.http.HttpServlet
import org.eclipse.jetty.servlet.{Holder => JettyHolder, FilterHolder, ServletHolder, ServletContextHandler}

/**
 * Type class for things that can be wrapped in a Jetty Holder and mounted
 * to a ServletContextHandler.
 */
trait Mountable[A] {
  type Holder <: JettyHolder[_ >: A]

  def mount(context: ServletContextHandler, path: String): Holder
}

object Mountable {
  implicit def servletToMountable(servlet: HttpServlet): Mountable[HttpServlet] = 
    new Mountable[HttpServlet] {
      type Holder = ServletHolder
      def mount(context: ServletContextHandler, pathSpec: String) = {
	val holder = new ServletHolder(servlet)
	context.addServlet(new ServletHolder(servlet), pathSpec)
	holder
      }
    }

  implicit def filterToMountable(filter: Filter)(implicit dispatches: EnumSet[DispatcherType]): Mountable[Filter] =
    new Mountable[Filter] {
      type Holder = FilterHolder
      def mount(context: ServletContextHandler, pathSpec: String) = {
	val holder = new FilterHolder(filter)
	context.addFilter(new FilterHolder(filter), pathSpec, dispatches)
	holder
      }
    }
}
    
