package org.scalatra
package servlet

import javax.servlet.{ ServletContext, ServletContextEvent, ServletContextListener }

import grizzled.slf4j.Logger
import org.scalatra.util.RicherString._

class ScalatraListener extends ServletContextListener {

  import org.scalatra.servlet.ScalatraListener._

  private[this] val logger: Logger = Logger[this.type]

  private[this] var cycle: LifeCycle = _

  private[this] var servletContext: ServletContext = _

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    try {
      configureServletContext(sce)
      configureCycleClass(Thread.currentThread.getContextClassLoader)
    } catch {
      case e: Throwable =>
        logger.error("Failed to initialize scalatra application at " + sce.getServletContext.getContextPath, e)
        throw e
    }
  }

  def contextDestroyed(sce: ServletContextEvent): Unit = {
    if (cycle != null) {
      logger.info("Destroying life cycle class: %s".format(cycle.getClass.getName))
      cycle.destroy(servletContext)
    }
  }

  protected def configureExecutionContext(sce: ServletContextEvent): Unit = {
  }

  protected def probeForCycleClass(classLoader: ClassLoader): (String, LifeCycle) = {
    val cycleClassName =
      Option(servletContext.getInitParameter(LifeCycleKey)).flatMap(_.blankOption) getOrElse DefaultLifeCycle
    logger info ("The cycle class name from the config: " + (if (cycleClassName == null) "null" else cycleClassName))

    val lifeCycleClass: Class[_] = try { Class.forName(cycleClassName, true, classLoader) } catch { case _: ClassNotFoundException => null; case t: Throwable => throw t }
    def oldLifeCycleClass: Class[_] = try { Class.forName(OldDefaultLifeCycle, true, classLoader) } catch { case _: ClassNotFoundException => null; case t: Throwable => throw t }
    val cycleClass: Class[_] = if (lifeCycleClass != null) lifeCycleClass else oldLifeCycleClass

    assert(cycleClass != null, "No lifecycle class found!")
    assert(classOf[LifeCycle].isAssignableFrom(cycleClass), "This is no lifecycle class.")
    logger debug "Loaded lifecycle class: %s".format(cycleClass)

    if (cycleClass.getName == OldDefaultLifeCycle)
      logger.warn("The Scalatra name for a boot class will be removed eventually. Please use ScalatraBootstrap instead as class name.")
    (cycleClass.getSimpleName, cycleClass.newInstance.asInstanceOf[LifeCycle])
  }

  protected def configureServletContext(sce: ServletContextEvent): Unit = {
    servletContext = sce.getServletContext
  }

  protected def configureCycleClass(classLoader: ClassLoader): Unit = {
    val (cycleClassName, cycleClass) = probeForCycleClass(classLoader)
    cycle = cycleClass
    logger.info("Initializing life cycle class: %s".format(cycleClassName))
    cycle.init(servletContext)
  }
}

object ScalatraListener {

  // DO NOT RENAME THIS CLASS NAME AS IT BREAKS THE ENTIRE WORLD
  // TOGETHER WITH THE WORLD IT WILL BREAK ALL EXISTING SCALATRA APPS
  // RENAMING THIS CLASS WILL RESULT IN GETTING SHOT, IF YOU SURVIVE YOU WILL BE SHOT AGAIN
  val DefaultLifeCycle: String = "ScalatraBootstrap"
  val OldDefaultLifeCycle: String = "Scalatra"
  val LifeCycleKey: String = "org.scalatra.LifeCycle"

}
