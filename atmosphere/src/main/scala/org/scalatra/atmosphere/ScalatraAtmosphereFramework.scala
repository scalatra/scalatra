package org.scalatra
package atmosphere

import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.container.{JBossWebCometSupport, TomcatCometSupport, Tomcat7CometSupport}

class ScalatraAtmosphereFramework(isFilter: Boolean = false, autoDetectHandlers: Boolean = false) extends AtmosphereFramework(isFilter, autoDetectHandlers) {

  def setupTomcat7() {
    if (!getAsyncSupport.supportWebSocket) {
      if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
        asyncSupport.synchronized {
          asyncSupport = new Tomcat7CometSupport(config)
        }
      }
    }
  }

  def setupTomcat() {
    if (!getAsyncSupport.supportWebSocket) {
      if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
        asyncSupport.synchronized {
          asyncSupport = new TomcatCometSupport(config)
        }
      }
    }
  }

  def setupJBoss() {
    if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
      asyncSupport.synchronized {
        asyncSupport = new JBossWebCometSupport(config)
      }
    }
  }
}