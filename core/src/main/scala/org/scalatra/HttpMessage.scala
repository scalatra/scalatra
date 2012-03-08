package org.scalatra

trait HttpMessage {
  def characterEncoding: Option[String]

  def contentType: Option[String]

  def header(name: String): Option[String]
}
