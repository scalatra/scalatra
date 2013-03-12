package org.scalatra
package atmosphere

import _root_.akka.actor._
import _root_.akka.dispatch._
import collection.JavaConverters._
import grizzled.slf4j.Logger
import org.atmosphere.cpr._
import java.net.URI

final class ScalatraBroadcaster(name: String, uri: URI, config: AtmosphereConfig)(implicit wireFormat: WireFormat, system: ActorSystem) extends DefaultBroadcaster(name, uri, config) {

  def this(name: String, config: AtmosphereConfig)(implicit wireFormat: WireFormat, system: ActorSystem) = this(name, URI.create("http://localhost"), config)

  private[this] val logger: Logger = Logger[ScalatraBroadcaster]

  def broadcast[T <: OutboundMessage](msg: T, clientFilter: ClientFilter): Future[T] = {
    logger.debug("There are %s resources in the [%s] broadcaster.".format(resources.size(), name))
    val selectedResources = resources.asScala map (_.client) filter clientFilter
    logger.info("Sending %s to %s".format(msg, selectedResources.map(_.uuid)))
    broadcast(wireFormat.render(msg), selectedResources.map(_.resource).toSet.asJava).map(_ => msg)
  }


}