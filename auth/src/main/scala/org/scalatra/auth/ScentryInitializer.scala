package org.scalatra.auth

import javax.servlet.{ ServletContextEvent, ServletContextListener }

/**
 * This seems like an ideal place to register global strategies
 */
abstract class ScentryInitializer extends ServletContextListener {
  def contextDestroyed(e: ServletContextEvent): Unit = {
    Scentry.globalStrategies.clear
  }

  def contextInitialized(e: ServletContextEvent): Unit
}
