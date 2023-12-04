package org.scalatra.cache

import jakarta.servlet.http.{ HttpServletResponse, HttpServletRequest }

trait KeyStrategy {
  def key(implicit request: HttpServletRequest, response: HttpServletResponse): String
}
