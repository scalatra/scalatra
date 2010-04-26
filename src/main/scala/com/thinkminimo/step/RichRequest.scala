package com.thinkminimo.step

import javax.servlet.http._

case class RichRequest(r: HttpServletRequest) {
  import RichRequest._

  @deprecated
  def host = r.getServerName

  @deprecated
  def port = Integer.toString(r.getServerPort)

  def referer = r.getHeader("Referer") match {
    case s: String => Some(s)
    case null => None
  }
}

