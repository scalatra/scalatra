package org.scalatra
package http

import java.net.URI
import scala.collection.mutable

trait HttpRequest extends HttpMessage with mutable.Map[String, AnyRef] {
  def uri: URI

  def isSecure: Boolean

  def method: HttpMethod

  def parameters: ScalatraKernel.MultiParams
}
