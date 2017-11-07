package org.scalatra
package atmosphere

import org.atmosphere.container._
import org.atmosphere.cpr.AtmosphereFramework

class ScalatraAtmosphereFramework(isFilter: Boolean = false, autoDetectHandlers: Boolean = false) extends AtmosphereFramework(isFilter, autoDetectHandlers) {

  def setupTomcat7(): Unit = {
    if (!getAsyncSupport.supportWebSocket) {
      if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
        asyncSupport.synchronized {
          asyncSupport = new Tomcat7CometSupport(config)
        }
      }
    }
  }

  def setupTomcat(): Unit = {
    if (!getAsyncSupport.supportWebSocket) {
      if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
        asyncSupport.synchronized {
          asyncSupport = new TomcatCometSupport(config)
        }
      }
    }
  }

  def setupJBoss(): Unit = {
    if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
      asyncSupport.synchronized {
        asyncSupport = new JBossWebCometSupport(config)
      }
    }
  }

  def enableSessionSupport() = sessionSupport(true)
  def disableSessionSupport() = sessionSupport(false)

}