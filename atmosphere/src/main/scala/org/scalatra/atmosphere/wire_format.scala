package org.scalatra
package atmosphere

import org.json4s.{ JsonAST, jackson, Formats }
import org.json4s.jackson.JsonMethods
import org.json4s.JsonAST.JValue

/**
 * The interface trait for a wire format.
 * Creating a new wire format means implementing these 3 methods.
 */
trait WireFormat {

  /**
   * The name of this wire format
   * @return The name
   */
  def name: String

  /**
   * A flag to indicate whether this wireformat supports acking or not
   * @return True if this wire format supports acking, otherwise false
   */
  def supportsAck: Boolean

  /**
   * Parse an inbound message from a string. This is used when a message is received over a connection.
   *
   * @param message The serialized message to parse
   * @return the resulting [[org.scalatra.atmosphere.InboundMessage]]
   */
  def parseInMessage(message: String): InboundMessage

  /**
   * Parse an outbound message from a string. This is used when the buffer is being drained.
   *
   * @param message The serialized message to parse
   * @return the resulting [[org.scalatra.atmosphere.OutboundMessage]]
   */
  def parseOutMessage(message: String): OutboundMessage

  /**
   * Render an outbound message to string. This is used when a message is sent to the remote party.
   *
   * @param message The message to serialize
   * @return The string representation of the message
   */
  def render(message: OutboundMessage): String

}

/**
 * A protocol format that is just plain and simple json. This protocol doesn't support acking.
 * It looks at the first character in the message and if it thinks it's JSON it will try to parse it as JSON
 * otherwise it creates a text message
 *
 */
abstract class SimpleJsonWireFormat extends WireFormat { self: org.json4s.JsonMethods[_] =>

  val name = "simpleJson"
  val supportsAck = false

  private[this] def parseMessage(message: String) = {
    if (message.trim.startsWith("{") || message.trim.startsWith("["))
      parseOpt(message) map (JsonMessage(_)) getOrElse TextMessage(message)
    else TextMessage(message)
  }

  def parseOutMessage(message: String): OutboundMessage = parseMessage(message)

  def parseInMessage(message: String): InboundMessage = parseMessage(message)

  def render(message: OutboundMessage) = message match {
    case TextMessage(text) => text
    case JsonMessage(json) => renderJson(json)
    case _ => ""
  }

  protected def renderJson(json: JValue): String
}