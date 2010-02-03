package com.thinkminimo.step

import util.matching.Regex
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

object StepRequest {
  val HostPortRegex = new Regex("([^:]*):?(.*)")
}

case class StepRequest(r: HttpServletRequest) extends HttpServletRequestWrapper(r) {
  import StepRequest._

  lazy val List(host, port) = r.getHeader("Host") match {
    case null => List("","");
    case HostPortRegex(x,y) => List(x,y)
  }
  def referer = r.getHeader("Referer")
}

