package com.thinkminimo.step

import util.matching.Regex
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

object RichRequest {
  val HostPortRegex = new Regex("([^:]*):?(.*)")
}

case class RichRequest(r: HttpServletRequest) {
  import RichRequest._

  lazy val List(host, port) = r.getHeader("Host") match {
    case null => List("","");
    case HostPortRegex(x,y) => List(x,y)
  }
  def referer = r.getHeader("Referer")
}

