package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.{ServletContextEvent, ServletContextListener}
import grizzled.slf4j.Logger
import util.RicherString._
import scala.util.control.Exception._

class ScalatraListener extends ServletContextListener {
  import ScalatraListener._

  private[this] val logger: Logger = Logger[this.type]
  
  private[this] var cycle: LifeCycle = _

  private[this] var servletContext: ServletContext = _

  def contextInitialized(sce: ServletContextEvent) {
    configureServletContext(sce)
    configureCycleClass()
  }

  def contextDestroyed(sce: ServletContextEvent) {
    if (cycle != null) {
      logger.info("Destroying life cycle class: %s".format(cycle.getClass.getName))
      cycle.destroy(servletContext)
    }
  }

  protected def probeForCycleClass() = {
    val cycleClassName = 
      Option(servletContext.getAttribute(LifeCycleKey)).flatMap(_.asInstanceOf[String].blankOption) getOrElse DefaultLifeCycle

    val lifeCycleClass: Class[_] = try { Class.forName(cycleClassName) } catch { case _ => null }
    def oldLifeCycleClass: Class[_] = try { Class.forName(OldDefaultLifeCycle) } catch { case _ => null }
    val cycleClass: Class[_] = if (lifeCycleClass != null) lifeCycleClass else oldLifeCycleClass

    if (cycleClass != null && !classOf[LifeCycle].isAssignableFrom(cycleClass)) {
      logger.error("This is no lifecycle class.")
      throw new ClassCastException("This is no lifecycle class.")
    }
    if (cycleClass.getName == OldDefaultLifeCycle)
      logger.warn("The Scalatra name for a boot class will be removed eventually. Please use ScalatraBootstrap instead as class name.")
    (cycleClass.getSimpleName, cycleClass.newInstance.asInstanceOf[LifeCycle])
  }

  protected def configureServletContext(sce: ServletContextEvent) {
    servletContext = sce.getServletContext
  }

  protected def configureCycleClass() {
    val (cycleClassName, cycleClass) = probeForCycleClass()
    cycle = cycleClass
    logger.info("Initializing life cycle class: %s".format(cycleClassName))
    cycle.init(servletContext)
  }
}

object ScalatraListener {
  
  // DO NOT RENAME THIS CLASS NAME AS IT BREAKS THE ENTIRE WORLD
  // TOGETHER WITH THE WORLD IT WILL BREAK ALL EXISTING SCALATRA APPS
  // RENAMING THIS CLASS WILL RESULT IN GETTING SHOT, IF YOU SURVIVE YOU WILL BE SHOT AGAIN
  val DefaultLifeCycle = "ScalatraBootstrap"
  val OldDefaultLifeCycle = "Scalatra"
  val LifeCycleKey = "org.scalatra.LifeCycle"
}
