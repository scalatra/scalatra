package org.scalatra

object HttpMessage {
  def isKeepAlive(msg: HttpMessage): Boolean = {
    (msg.headers.get("Connection").map(_.toLowerCase), msg.serverProtocol) match {
      case (Some("close"), _) => false
      case (Some("keep-alive"), Http10) => true
      case (_, proto) if proto.keepAliveDefault => true
      case (_, _) => false
    }
  }
}
trait HttpMessage {

  /**
   * The version of the protocol the client used to send the request.
   * Typically this will be something like "HTTP/1.0"  or "HTTP/1.1" and may
   * be used by the application to determine how to treat any HTTP request
   * headers.
   */
  def serverProtocol: HttpVersion

  /**
   * A map of headers.  Multiple header values are separated by a ','
   * character.  The keys of this map are case-insensitive.
   */
  def headers: scala.collection.Map[String, String]

  /**
   * The content of the Content-Type header, or None if absent.
   */
  def contentType: Option[String]

  /**
   * Returns the name of the character encoding of the body, or None if no
   * character encoding is specified.
   */
  def characterEncoding: Option[String]
}
