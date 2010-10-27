package org.scalatra

import scala.collection.{Map => CMap}
import scala.io.Source
import javax.servlet.http.HttpServletRequest
import util.MultiMapHeadView
import java.util.Locale

case class RichRequest(r: HttpServletRequest) extends AttributesMap {
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

  def isAjax: Boolean = r.getHeader("X-Requested-With") != null
  def isWrite: Boolean = ScalatraKernel.writeMethods.contains(r.getMethod.toUpperCase(Locale.ENGLISH))

  def multiCookies: CMap[String, Seq[String]] =
    Option(r.getCookies).getOrElse(Array()).toSeq.
      groupBy { _.getName }.
      transform { case(k, v) => v map { _.getValue }}.
      withDefaultValue(Seq.empty)

  def cookies: CMap[String, String] = new MultiMapHeadView[String, String] { protected def multiMap = multiCookies }

  protected def attributes = r
}

