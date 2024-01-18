package org.scalatra.cache

import org.scalatra.ServletCompat.http.{ HttpServletResponse, HttpServletRequest }

object DefaultHeaderStrategy extends HeaderStrategy {
  override def isUnchanged(revision: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Boolean = {
    revision == request.getHeader("ETag")
  }

  override def setRevision(revision: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setHeader("ETag", revision)
  }

  override def getNewRevision(): String = {
    System.currentTimeMillis().toString
  }
}
