package org.scalatra.cache

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }

trait KeyStrategy {
  def key(implicit request: HttpServletRequest, response: HttpServletResponse): String
}
