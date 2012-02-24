package org.scalatra

trait HttpMessage {
  def characterEncoding: Option[String]
  def characterEncoding_=(encoding: Option[String]): Unit

  def contentType: Option[String]

  def header(name: String): Option[String]
}
