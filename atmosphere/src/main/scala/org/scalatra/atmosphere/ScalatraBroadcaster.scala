package org.scalatra
package atmosphere

import _root_.akka.actor._
import _root_.akka.dispatch._
import _root_.akka.util.Deadline
import _root_.akka.util.duration._
import collection.JavaConverters._
import grizzled.slf4j.Logger
import org.atmosphere.cpr._
import org.json4s.Formats
import scala.util.control.Exception.allCatch


final class ScalatraBroadcaster(id: String, config: AtmosphereConfig)(implicit formats: Formats, system: ActorSystem) extends DefaultBroadcaster(id, config) {

  private[this] val logger: Logger = Logger[ScalatraBroadcaster]

  def broadcast[T <: OutboundMessage](msg: T, clientFilter: ClientFilter): Future[T] = {
    val wireFormat: WireFormat = new SimpleJsonWireFormat
    val selectedResources = resources.asScala map (_.client) filter clientFilter
    logger.trace("Sending %s to %s".format(msg, selectedResources.map(_.uuid)))
    broadcast(wireFormat.render(msg), selectedResources.map(_.resource).toSet.asJava).map(_ => msg)
  }


}