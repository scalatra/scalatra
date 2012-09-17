package org.scalatra
package atmosphere

import java.util.UUID
import org.atmosphere.cpr.{Broadcaster, AtmosphereResource}
import collection.JavaConverters._

trait AtmosphereClient {

  private[atmosphere] var resource: AtmosphereResource = _

  def uuid: UUID

  def receive: AtmoReceive

  def send(msg: OutboundMessage): Unit = {
    resource.getBroadcaster.broadcast(msg)
  }

  def broadcast(msg: OutboundMessage, filterClause: Broadcaster => Boolean): Unit = {
    resource.getAtmosphereConfig.getBroadcasterFactory.lookupAll().asScala filter filterClause foreach (_.broadcast(msg ))
  }

}