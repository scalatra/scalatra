package org.scalatra
package http

import java.net.URI
import scala.collection.mutable

trait HttpRequest[A] extends HttpMessage[A] {
  def default: A

  def uri(implicit a: A): URI

  def isSecure(implicit a: A): Boolean

  def method(implicit a: A): HttpMethod

  def parameters(implicit a: A): ScalatraKernel.MultiParams

  def get(key: String)(implicit a: A): Option[Any]
  def apply(key: String)(implicit a: A): Any = get(key).get
  def update(key: String, value: Any)(implicit a: A): Unit
}
