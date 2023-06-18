package org.scalatra.cache

import org.scalatra.ServletCompat.http.{ HttpServletResponse, HttpServletRequest }
import scala.util.hashing.MurmurHash3

object DefaultKeyStrategy extends KeyStrategy {
  override def key(implicit request: HttpServletRequest, response: HttpServletResponse): String = {
    MurmurHash3.stringHash(request.getPathInfo + request.getQueryString).toString
  }
}
