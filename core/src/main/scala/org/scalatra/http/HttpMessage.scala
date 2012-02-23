package org.scalatra
package http

trait HttpMessage[A] {
  def characterEncoding(implicit a: A): Option[String]
  def characterEncoding_=(encoding: Option[String])(implicit a: A): Unit

  def contentType(implicit a: A): Option[String]

  def header(name: String)(implicit a: A): Option[String]
}
