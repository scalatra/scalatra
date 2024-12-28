package org.scalatra.cache

import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}

object DefaultHeaderStrategy extends HeaderStrategy {
  override def isUnchanged(
      revision: String
  )(implicit request: HttpServletRequest, response: HttpServletResponse) =
    revision == request.getHeader("ETag")

  override def setRevision(
      revision: String
  )(implicit request: HttpServletRequest, response: HttpServletResponse) =
    response.setHeader("ETag", revision)

  override def getNewRevision(): String =
    System.currentTimeMillis().toString
}
