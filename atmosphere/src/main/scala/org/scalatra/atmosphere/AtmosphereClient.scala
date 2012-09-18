package org.scalatra
package atmosphere

import org.atmosphere.cpr._
import org.json4s.Formats

trait AtmosphereClient {

  @volatile private[atmosphere] var resource: AtmosphereResource = _
  final protected def SkipSelf: ClientFilter = _.uuid() != uuid
  final protected def OnlySelf: ClientFilter = _.uuid() == uuid

  final def uuid: String = resource.uuid()

  def receive: AtmoReceive

  final def send(msg: OutboundMessage)(implicit formats: Formats) = broadcast(msg, OnlySelf)

  final def broadcast(msg: OutboundMessage, filter: ClientFilter = SkipSelf)(implicit formats: Formats) = {
    val broadcaster = BroadcasterFactory.getDefault.lookup(classOf[ScalatraBroadcaster], "/*").asInstanceOf[ScalatraBroadcaster]
    broadcaster.broadcast(msg, filter)
  }

}