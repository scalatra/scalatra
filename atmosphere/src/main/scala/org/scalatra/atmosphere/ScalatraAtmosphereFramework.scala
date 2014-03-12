package org.scalatra
package atmosphere

import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.container._
import grizzled.slf4j.Logger


class ScalatraAtmosphereFramework(isFilter: Boolean = false, autoDetectHandlers: Boolean = false) extends AtmosphereFramework(isFilter, autoDetectHandlers) {

  private[this] val logger = Logger[ScalatraAtmosphereFramework]
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

  def enableSessionSupport() = sessionSupport(true)
  def disableSessionSupport() = sessionSupport(false)

}