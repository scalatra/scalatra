package org.scalatra
package servlet

import javax.servlet.{ServletContextEvent, ServletContextListener}
import grizzled.slf4j.Logger

class ScalatraListener extends ServletContextListener {
  import ScalatraListener._

  private val logger: Logger = Logger[this.type]
  
  private var cycle: LifeCycle = _

  private var appCtx: ServletApplicationContext = _

  def contextInitialized(sce: ServletContextEvent) = {
    appCtx = ServletApplicationContext(sce.getServletContext)
    val cycleClassName = 
      Option(appCtx.getInitParameter(LifeCycleKey)) getOrElse DefaultLifeCycle
    val cycleClass = Class.forName(cycleClassName)
    cycle = cycleClass.newInstance.asInstanceOf[LifeCycle]
    logger.info("Initializing life cycle class: %s".format(cycleClassName))
    cycle.init(appCtx)
  }

  def contextDestroyed(sce: ServletContextEvent) = {
    if (cycle != null) {
      logger.info("Destroying life cycle class: %s".format(cycle.getClass.getName))
      cycle.destroy(appCtx)
    }
  }
}

object ScalatraListener {
  val DefaultLifeCycle = "Scalatra"
  val LifeCycleKey = "org.scalatra.LifeCycle"
}
