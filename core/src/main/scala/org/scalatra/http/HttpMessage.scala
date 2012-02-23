package org.scalatra
package http

trait HttpMessage[A] {
  def characterEncoding(implicit a: A): String
  def characterEncoding_=(encoding: String)(implicit a: A): Unit

  def contentType(implicit a: A): String
}
