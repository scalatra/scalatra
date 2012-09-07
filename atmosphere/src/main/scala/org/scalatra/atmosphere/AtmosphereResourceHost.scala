package org.scalatra
package atmosphere

import org.atmosphere.cpr.{AtmosphereResourceEvent, AtmosphereHandler, AtmosphereResource}

class AtmosphereResourceHost(resource: AtmosphereResource) {

}

class ScalatraAtmoHandler extends AtmosphereHandler {
  def onRequest(resource: AtmosphereResource) {}

  def onStateChange(event: AtmosphereResourceEvent) {}

  def destroy() {}
}