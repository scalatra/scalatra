package org.scalatra
package atmosphere

import org.atmosphere.cpr._
import org.json4s._
import org.scalatra.util.RicherString._


trait AtmosphereClient {

  @volatile private[atmosphere] var resource: AtmosphereResource = _

  final protected def SkipSelf: ClientFilter = _.uuid() != uuid
  final protected def OnlySelf: ClientFilter = _.uuid() == uuid
  final protected val Everyone: ClientFilter = _ => true

  protected def requestUri = {
    val u = resource.getRequest.getRequestURI.blankOption getOrElse "/"
    if (u.endsWith("/")) u + "*" else u + "/*"
  }

  final def uuid: String = resource.uuid()

  def receive: AtmoReceive

  final def send(msg: OutboundMessage) = broadcast(msg, OnlySelf)

  final def broadcast(msg: OutboundMessage, filter: ClientFilter = SkipSelf) = {
    val broadcaster = resource.getBroadcaster.asInstanceOf[ScalatraBroadcaster]
    broadcaster.broadcast(msg, filter)
  }



}