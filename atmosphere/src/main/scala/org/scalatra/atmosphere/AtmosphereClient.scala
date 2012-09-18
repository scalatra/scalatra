package org.scalatra
package atmosphere

import java.util.UUID
import org.atmosphere.cpr.{Broadcaster, AtmosphereResource}
import collection.JavaConverters._

trait AtmosphereClient {

  @volatile private[atmosphere] var resource: AtmosphereResource = _
  final protected def SkipSelf: ClientFilter = _.uuid() != uuid
  final protected def OnlySelf: ClientFilter = _.uuid() == uuid

  final def uuid: String = resource.uuid()

  def receive: AtmoReceive

  final def send(msg: OutboundMessage) = {
    resource.getBroadcaster.asInstanceOf[ScalatraBroadcaster].broadcast(msg, OnlySelf)
  }

  final def broadcast(msg: OutboundMessage, filter: ClientFilter = SkipSelf) = {
    resource.getBroadcaster.asInstanceOf[ScalatraBroadcaster].broadcast(msg, SkipSelf)
  }

}