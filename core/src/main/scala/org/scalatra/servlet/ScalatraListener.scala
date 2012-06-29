package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.{ServletContextEvent, ServletContextListener}
import grizzled.slf4j.Logger

class ScalatraListener extends ServletContextListener {
  import ScalatraListener._

  private val logger: Logger = Logger[this.type]
  
  private var cycle: LifeCycle = _

  private var servletContext: ServletContext = _

  def contextInitialized(sce: ServletContextEvent) {
    servletContext = sce.getServletContext
    val cycleClassName = 
      Option(servletContext.getInitParameter(LifeCycleKey)) getOrElse DefaultLifeCycle
    val cycleClass = Class.forName(cycleClassName)
    cycle = cycleClass.newInstance.asInstanceOf[LifeCycle]
    logger.info("Initializing life cycle class: %s".format(cycleClassName))
    cycle.init(servletContext)
  }

  def contextDestroyed(sce: ServletContextEvent) {
    if (cycle != null) {
      logger.info("Destroying life cycle class: %s".format(cycle.getClass.getName))
      cycle.destroy(servletContext)
    }
  }
}

object ScalatraListener {
  val DefaultLifeCycle = "Scalatra"
  val LifeCycleKey = "org.scalatra.LifeCycle"
}
