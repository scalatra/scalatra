package org.scalatra
package atmosphere

import org.atmosphere.cpr.{ Action => AtmoAction, BroadcasterFactory, AtmosphereResponse, AtmosphereRequest, AtmosphereFramework }
import org.atmosphere.container._
import java.util.UUID
import org.atmosphere.cpr.ApplicationConfig._
import org.atmosphere.cpr.FrameworkConfig._
import org.atmosphere.cpr.HeaderConfig._
import org.atmosphere.websocket.WebSocket._
import org.scalatra.servlet.ServletApiImplicits._
import scala.util.control.Exception._
import org.scalatra.util.RicherString._
import grizzled.slf4j.Logger
import scala.Some

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