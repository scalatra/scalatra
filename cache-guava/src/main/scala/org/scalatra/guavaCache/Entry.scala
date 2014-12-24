package org.scalatra.guavaCache

import org.joda.time.DateTime

case class Entry[+A](value: A, expiresAt: Option[DateTime]) {
  def isExpired: Boolean = expiresAt.exists(_.isBeforeNow)
}
