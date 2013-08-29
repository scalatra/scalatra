package org.scalatra.atmosphere

import _root_.akka.actor._
import org.atmosphere.cpr._
import java.util.concurrent.ConcurrentLinkedQueue
import org.atmosphere.plugin.redis.RedisBroadcaster


final class RedisScalatraBroadcaster(id: String, config: AtmosphereConfig)
                                      (implicit wireFormat: WireFormat, system: ActorSystem)
  extends RedisBroadcaster(id, config) with ScalatraBroadcaster {
  protected var _resources: ConcurrentLinkedQueue[AtmosphereResource] = resources
  protected var _wireFormat: WireFormat = wireFormat
  protected implicit var _actorSystem: ActorSystem = system
}