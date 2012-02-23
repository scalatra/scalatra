package org.scalatra
package http

import scala.collection.mutable

trait HttpRequest[A] extends HttpMessage[A] {
  def default: A

  def method(implicit a: A): HttpMethod

  def parameters(implicit a: A): ScalatraKernel.MultiParams
 
  def apply(key: String)(implicit a: A): Any
  def update(key: String, value: Any)(implicit a: A): Unit
}
