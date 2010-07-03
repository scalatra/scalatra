package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait Handler {
  def handle(req: HttpServletRequest, res: HttpServletResponse): Unit
}