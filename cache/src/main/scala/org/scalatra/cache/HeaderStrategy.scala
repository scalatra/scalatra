package org.scalatra.cache

import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}

trait HeaderStrategy {
  def isUnchanged(revision: String)(implicit
      request: HttpServletRequest,
      response: HttpServletResponse
  ): Boolean
  def setRevision(
      revision: String
  )(implicit request: HttpServletRequest, response: HttpServletResponse): Unit
  def getNewRevision(): String
}
