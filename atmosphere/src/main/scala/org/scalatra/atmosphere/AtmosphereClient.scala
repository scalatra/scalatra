package org.scalatra
package atmosphere

import grizzled.slf4j.Logger
import org.atmosphere.cpr._
import org.scalatra.util.RicherString._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

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
    val res: Broadcaster = BroadcasterFactory.getDefault.lookup(norm)
    if (res != null && res.isInstanceOf[ScalatraBroadcaster]) {
      Some(res.asInstanceOf[ScalatraBroadcaster])
    } else {
      None
    }
  }

  def broadcast(path: String, message: OutboundMessage, filter: ClientFilter = new Everyone)(implicit executionContext: ExecutionContext) = {
    lookup(path) foreach { _.broadcast(message, filter) }
  }

  def broadcastAll(message: OutboundMessage, filter: ClientFilter = new Everyone)(implicit executionContext: ExecutionContext) = {
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
trait AtmosphereClient extends AtmosphereClientFilters {

  @volatile private[atmosphere] var resource: AtmosphereResource = _
  @transient private[this] val internalLogger = Logger[AtmosphereClient]
  @transient private[this] def broadcaster = resource.getBroadcaster.asInstanceOf[ScalatraBroadcaster]

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
