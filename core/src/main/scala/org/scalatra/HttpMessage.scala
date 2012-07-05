package org.scalatra

trait HttpMessage {
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
