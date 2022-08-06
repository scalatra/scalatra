package org.scalatra.cache

import jakarta.servlet.http.{ HttpServletResponse, HttpServletRequest }

trait HeaderStrategy {
  def isUnchanged(revision: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Boolean
  def setRevision(revision: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit
  def getNewRevision(): String
}
