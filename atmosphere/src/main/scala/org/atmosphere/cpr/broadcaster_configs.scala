package org.atmosphere.cpr

import java.net.URI

import org.scalatra.atmosphere.{ RedisScalatraBroadcaster, ScalatraBroadcaster }

trait BroadcasterConf {
  def broadcasterClass: Class[_ <: ScalatraBroadcaster]
  def uri: URI
  def extraSetup: Broadcaster => Unit // To perform optional plugin-specific Broadcaster setup
}

/**
 *
 * Basic Configuration-holder for Scalatra Atmosphere Broadcaster configuration
 * @param broadcasterClass Class[_<:ScalatraBroadcaster]
 * @param uri [[URI]] defaults to http://127.0.0.1
 * @param extraSetup Broadcaster => Unit Function that is passed an initialized [[Broadcaster]] in order to allow for
 *                   optional plugin-specific Broadcaster setup. Defaults to doing nothing.
 */
sealed case class ScalatraBroadcasterConfig(broadcasterClass: Class[_ <: ScalatraBroadcaster],
  uri: URI = URI.create("http://127.0.0.1"),
  extraSetup: Broadcaster => Unit = { b => }) extends BroadcasterConf

/**
 * Convenient configuration class for RedisBroadcaster
 *
 * Using this class will automatically take care of setting Redis auth on the underlying
 * RedisBroadcaster if the auth parameter is given an argument
 *
 * @param uri [[URI]] for the Redis Server. Defaults to redis://127.0.0.1:6379
 * @param auth An Option[String] if the Redis Server requires a password. Defaults to None
 */
sealed case class RedisScalatraBroadcasterConfig(uri: URI = URI.create("redis://127.0.0.1:6379"), auth: Option[String] = None) extends BroadcasterConf {
  final def broadcasterClass = classOf[RedisScalatraBroadcaster]
  final def extraSetup = { b: Broadcaster =>
    auth.foreach(b.asInstanceOf[RedisScalatraBroadcaster].setAuth(_))
  }
}