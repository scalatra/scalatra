package org.scalatra.auth

import org.scalatra.ServletCompat.{ ServletContextEvent, ServletContextListener }

/**
 * This seems like an ideal place to register global strategies
 */
abstract class ScentryInitializer extends ServletContextListener {
  override def contextDestroyed(e: ServletContextEvent): Unit = {
    Scentry.globalStrategies.clear()
  }

  override def contextInitialized(e: ServletContextEvent): Unit = {
  }
}
