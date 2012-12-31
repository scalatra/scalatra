package org.scalatra
package atmosphere

import _root_.akka.actor._
import collection.JavaConverters._
import grizzled.slf4j.Logger
import org.atmosphere.cpr._
import org.json4s.Formats
import concurrent.{ExecutionContext, Future}


final class ScalatraBroadcaster(id: String, config: AtmosphereConfig)(implicit wireFormat: WireFormat, system: ActorSystem) extends DefaultBroadcaster(id, config) {

  private[this] val logger: Logger = Logger[ScalatraBroadcaster]

  def broadcast[T <: OutboundMessage](msg: T, clientFilter: ClientFilter)(implicit executionContext: ExecutionContext): Future[T] = {
    val selectedResources = resources.asScala map (_.client) filter clientFilter
    logger.trace("Sending %s to %s".format(msg, selectedResources.map(_.uuid)))
    broadcast(wireFormat.render(msg), selectedResources.map(_.resource).toSet.asJava).map(_ => msg)
  }


}