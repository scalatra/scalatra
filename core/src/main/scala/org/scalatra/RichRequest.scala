package org.scalatra

import javax.servlet.http._
import io.Source
import scala.collection.{Map => CMap}

case class RichRequest(r: HttpServletRequest) {
  import RichRequest._

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

  def multiCookies: CMap[String, Seq[String]] =
    r.getCookies.toSeq groupBy { _.getName } transform { case(k, v) => v map { _.getValue }} withDefaultValue Seq.empty

  def cookies: CMap[String, String] = new MultiMapHeadView[String, String] { protected def multiMap = multiCookies }
}

