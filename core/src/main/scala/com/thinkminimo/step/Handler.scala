package com.thinkminimo.step

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait Handler {
  def handle(req: HttpServletRequest, res: HttpServletResponse): Unit
}