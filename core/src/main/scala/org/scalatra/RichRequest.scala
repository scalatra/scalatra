package org.scalatra

import javax.servlet.http.HttpServletRequest
import scala.collection.mutable.Map
import scala.io.Source

case class RichRequest(r: HttpServletRequest) {
  @deprecated(message = "Use HttpServletRequest.getServerName() instead")
  def host = r.getServerName

  @deprecated(message = "Use HttpServletRequest.getServerPort() instead")
  def port = Integer.toString(r.getServerPort)

  def referer = r.getHeader("Referer") match {
    case s: String => Some(s)
    case null => None
  }

  def body:String = {
    Source.fromInputStream(r.getInputStream).mkString
  }

  def attributes: Map[String, AnyRef] = new AttributesMap { def attributes = r }
}

