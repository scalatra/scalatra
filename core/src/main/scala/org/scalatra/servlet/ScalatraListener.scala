package org.scalatra
package servlet

import javax.servlet.{ServletContextEvent, ServletContextListener}

class ScalatraListener extends ServletContextListener {
  import ScalatraListener._

  private var cycle: LifeCycle = _

  def contextInitialized(sce: ServletContextEvent) = {
    val ctx = sce.getServletContext
    val cycleClassName = 
      Option(ctx.getInitParameter(LifeCycleKey)) getOrElse DefaultLifeCycle
    val cycleClass = Class.forName(cycleClassName)
    cycle = cycleClass.newInstance.asInstanceOf[LifeCycle]
    cycle.init(ctx)
  }

  def contextDestroyed(sce: ServletContextEvent) = {
    cycle.destroy(sce.getServletContext)
  }
}

object ScalatraListener {
  val DefaultLifeCycle = "Scalatra"
  val LifeCycleKey = "org.scalatra.LifeCycle"
}
