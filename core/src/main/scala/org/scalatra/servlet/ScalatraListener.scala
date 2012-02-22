package org.scalatra
package servlet

import javax.servlet.{ServletContextEvent, ServletContextListener}
import grizzled.slf4j.Logger

class ScalatraListener extends ServletContextListener {
  import ScalatraListener._

  private val logger: Logger = Logger[this.type]
  
  private var cycle: LifeCycle = _

  def contextInitialized(sce: ServletContextEvent) = {
    val ctx = sce.getServletContext
    val cycleClassName = 
      Option(ctx.getInitParameter(LifeCycleKey)) getOrElse DefaultLifeCycle
    val cycleClass = Class.forName(cycleClassName)
    cycle = cycleClass.newInstance.asInstanceOf[LifeCycle]
    logger.info("Initializing life cycle class: %s".format(cycleClassName))
    cycle.init(ctx)
  }

  def contextDestroyed(sce: ServletContextEvent) = {
    if (cycle != null) {
      logger.info("Destroying life cycle class: %s".format(cycle.getClass.getName))
      cycle.destroy(sce.getServletContext)
    }
  }
}

object ScalatraListener {
  val DefaultLifeCycle = "Scalatra"
  val LifeCycleKey = "org.scalatra.LifeCycle"
}
