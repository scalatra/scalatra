package org.scalatra
package atmosphere

import org.atmosphere.cpr._
import org.json4s._
import org.scalatra.util.RicherString._
import grizzled.slf4j.Logger

/**
 * Provides a handle for a single Atmosphere connection. 
 * 
 * Each browser or other device which connects to an `atmosphere` route is
 * assigned its own AtmosphereClient, with a uuid. This is a good bet for
 * subclassing if you need to implement your own message distribution logic.
 * Subclasses may define their own ClientFilter logic in addition to the
 * stock ClientFilters already defined, in order to segment message delivery.
 */
trait AtmosphereClient {

  @volatile private[atmosphere] var resource: AtmosphereResource = _
  private[this] val internalLogger = Logger[AtmosphereClient]

  /**
   * Deliver the message to everyone except the current user.
   */
  final protected def SkipSelf: ClientFilter = _.uuid != uuid
  
  /**
   * Deliver the message only to the current user.
   */
  final protected def OnlySelf: ClientFilter = _.uuid == uuid
  
  /**
   * Deliver the message to all connected users.
   */
  final protected val Everyone: ClientFilter = _ => true

  protected def requestUri = {
    val u = resource.getRequest.getRequestURI.blankOption getOrElse "/"
    if (u.endsWith("/")) u + "*" else u + "/*"
  }

  /**
   * A unique identifier for a given connection. Can be used for filtering
   * purposes.
   */
  final def uuid: String = resource.uuid()

  /**
   * Receive an inbound message. 
   */
  def receive: AtmoReceive

  /**
   * A convenience method which sends a message only to the current client,
   * using a broadcast filter.
   */
  final def send(msg: OutboundMessage) = broadcast(msg, OnlySelf)

  /**
   * Broadcast a message to all clients, skipping the current client by default
   * (i.e. normal chat server behaviour). Optionally filter the clients to
   * deliver the message to by applying a filter.
   */
  final def broadcast(msg: OutboundMessage, filter: ClientFilter = SkipSelf) = {
    if (resource == null) {
      internalLogger.warn("The resource is null, can't publish")
    }
    if (resource != null && resource.getBroadcaster == null)
      internalLogger.warn("The broadcaster is null, can't publish")
    val broadcaster = resource.getBroadcaster.asInstanceOf[ScalatraBroadcaster]
    broadcaster.broadcast(msg, filter)
  }

  final def disconnect() { resource.resume() }
}