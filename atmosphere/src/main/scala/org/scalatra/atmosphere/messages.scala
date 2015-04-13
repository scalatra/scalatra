package org.scalatra
package atmosphere

import org.json4s._

/**
 * A marker trait for inbound messages
 */
sealed trait InboundMessage

/**
 * A marker trait for outbound messages
 */
sealed trait OutboundMessage

/**
 * A base trait for creating messages of different content types
 * @tparam T The type of content this protocol message represents
 */
trait ProtocolMessage[T] extends InboundMessage with OutboundMessage {
  def content: T
}

/**
 * A callback event signaling that the connection has been fully established.
 * This means that any handshakes have been completed successfully too.
 *
 * When you receive this callback message you can be sure there is someone on the other end.
 */
case object Connected extends InboundMessage

/**
 * A callback event signaling a heartbeat
 *
 * When you receive this heartbeat message you can be sure there is someone on the other end.
 */
case object Heartbeat extends InboundMessage

/**
 * A message representing a json object sent to/received from a remote party.
 *
 * @param content A [[org.json4s.JValue]] object
 */
case class JsonMessage(content: JValue) extends ProtocolMessage[JValue]

/**
 * A message representing a text object sent to/received from a remote party.
 *
 * @param content A [[scala.Predef.String]] representing the content of the message
 */
case class TextMessage(content: String) extends ProtocolMessage[String]

/**
 * A message representing an array of bytes sent to/received from a remote party.
 *
 * @param content An Array of Bytes representing the content of the message
 */
case class BinaryMessage(content: Array[Byte]) extends ProtocolMessage[Array[Byte]]

/**
 * A callback event signaling that an error has occurred. if the error was an exception thrown
 * then the cause object will be filled in.
 *
 * @param cause A [[scala.Option]] of [[java.lang.Throwable]]
 */
case class Error(cause: Option[Throwable]) extends InboundMessage

sealed trait Disconnector
case object ServerDisconnected extends Disconnector
case object ClientDisconnected extends Disconnector
/**
 * A callback event signaling that the connection has ended, if the cause was an exception thrown
 * then the cause object will be filled in.
 *
 * @param cause A [[scala.Option]] of [[java.lang.Throwable]]
 */
case class Disconnected(who: Disconnector, cause: Option[Throwable]) extends InboundMessage
