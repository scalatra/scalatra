package org.scalatra.cache

import org.scalatra.ServletCompat.http.{ HttpServletResponse, HttpServletRequest }

trait KeyStrategy {
  def key(implicit request: HttpServletRequest, response: HttpServletResponse): String
}
