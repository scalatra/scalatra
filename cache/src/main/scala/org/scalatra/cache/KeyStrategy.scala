package org.scalatra.cache

import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}

trait KeyStrategy {
  def key(implicit
      request: HttpServletRequest,
      response: HttpServletResponse
  ): String
}
