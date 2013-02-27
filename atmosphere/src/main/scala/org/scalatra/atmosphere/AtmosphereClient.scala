package org.scalatra
package atmosphere

import org.atmosphere.cpr._
import org.json4s._
import org.scalatra.util.RicherString._
import grizzled.slf4j.Logger
import concurrent.ExecutionContext
import scala.collection.JavaConverters._

object AtmosphereClient {
  def lookupAll(): Seq[ScalatraBroadcaster] = {
    BroadcasterFactory.getDefault.lookupAll().asScala.toSeq collect {
      case b: ScalatraBroadcaster => b
    }
  }

  def lookup(path: String): Option[ScalatraBroadcaster] = {
    val pth = path.blankOption getOrElse "/*"
    val norm = if (!pth.endsWith("/*")) {
      if (!pth.endsWith("/")) pth + "/*" else "*"
    } else pth
    val res = BroadcasterFactory.getDefault.lookup(norm)
    if (res.isInstanceOf[ScalatraBroadcaster]) Option(res).map(_.asInstanceOf[ScalatraBroadcaster])
    else None
  }

  def broadcast(path: String, message: OutboundMessage, filter: ClientFilter = _ => true)(implicit executionContext: ExecutionContext) = {
    lookup(path) foreach { _.broadcast(message, filter) }
  }

  def broadcastAll(message: OutboundMessage, filter: ClientFilter = _ => true)(implicit executionContext: ExecutionContext) = {
    lookupAll() foreach {
      _ broadcast (message, filter)
    }
  }
}

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
  private[this] def broadcaster = resource.getBroadcaster.asInstanceOf[ScalatraBroadcaster]

  /**
   * Deliver the message to everyone except the current user.
   */
  final protected def SkipSelf: ClientFilter = _.uuid != uuid
  final protected def Others: ClientFilter = SkipSelf

  /**
   * Deliver the message only to the current user.
   */
  final protected def OnlySelf: ClientFilter = _.uuid == uuid

  final protected def Me: ClientFilter = OnlySelf

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
   * using a broadcast filter.  This is the same as calling `broadcast(message, to = Me)`
   */
  final def send(msg: OutboundMessage)(implicit executionContext: ExecutionContext) = broadcast(msg, to = Me)(executionContext)

  /**
   * A convenience method which sends a message only to the current client,
   * using a broadcast filter.
   */
  final def !(msg: OutboundMessage)(implicit executionContext: ExecutionContext) = send(msg)(executionContext)

  /**
   * Broadcast a message to all clients, skipping the current client by default
   * (i.e. normal chat server behaviour). Optionally filter the clients to
   * deliver the message to by applying a filter.
   */
  final def broadcast(msg: OutboundMessage, to: ClientFilter = Others)(implicit executionContext: ExecutionContext) = {
    if (resource == null)
      internalLogger.warn("The resource is null, can't publish")

    if (resource.getBroadcaster == null)
      internalLogger.warn("The broadcaster is null, can't publish")

    broadcaster.broadcast(msg, to)
  }

  /**
   * Broadcast a message to all clients, skipping the current client by default
   * (i.e. normal chat server behaviour). Optionally filter the clients to
   * deliver the message to by applying a filter.
   */
  final def ><(msg: OutboundMessage, to: ClientFilter = Others)(implicit executionContext: ExecutionContext) = broadcast(msg, to)(executionContext)

}
