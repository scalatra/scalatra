package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.{ServletContextEvent, ServletContextListener}
import grizzled.slf4j.Logger
import util.RicherString._

class ScalatraListener extends ServletContextListener {
  import ScalatraListener._

  private val logger: Logger = Logger[this.type]
  
  private var cycle: LifeCycle = _

  private var servletContext: ServletContext = _

  def contextInitialized(sce: ServletContextEvent) {
    servletContext = sce.getServletContext
    val cycleClassName = 
      Option(servletContext.getAttribute(LifeCycleKey)).flatMap(_.asInstanceOf[String].blankOption) getOrElse DefaultLifeCycle
    val cycleClass = Class.forName(cycleClassName)
    if (!classOf[LifeCycle].isAssignableFrom(cycleClass)) {
      logger.error("This is no lifecycle class.")
      throw new ClassCastException("This is no lifecycle class.")
    }
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
  val DefaultLifeCycle = "Bootstrap"
  val LifeCycleKey = "org.scalatra.LifeCycle"
}
