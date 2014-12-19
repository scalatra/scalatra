package org.scalatra.atmosphere

import _root_.akka.actor._
import org.atmosphere.cpr._
import java.util.concurrent.ConcurrentLinkedQueue

final class DefaultScalatraBroadcaster()(implicit wireFormat: WireFormat, system: ActorSystem)
    extends DefaultBroadcaster with ScalatraBroadcaster {

  protected var _resources: ConcurrentLinkedQueue[AtmosphereResource] = resources
  protected var _wireFormat: WireFormat = wireFormat
  protected implicit var _actorSystem: ActorSystem = system
}