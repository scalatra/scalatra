package org.scalatra
package atmosphere

import java.util.concurrent.ConcurrentLinkedQueue

import _root_.akka.actor._
import grizzled.slf4j.Logger
import org.atmosphere.cpr._

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

trait ScalatraBroadcaster extends Broadcaster {

  private[this] val logger: Logger = Logger[ScalatraBroadcaster]
  protected var _resources: ConcurrentLinkedQueue[AtmosphereResource]
  protected var _wireFormat: WireFormat
  protected implicit var _actorSystem: ActorSystem

  def broadcast[T <: OutboundMessage](msg: T, clientFilter: ClientFilter)(implicit executionContext: ExecutionContext): Future[T] = {
    val selectedResources = _resources.asScala filter clientFilter
    logger.trace("Sending %s to %s".format(msg, selectedResources.map(_.uuid)))
    broadcast(_wireFormat.render(msg), selectedResources.toSet.asJava).map(_ => msg)
  }

}
