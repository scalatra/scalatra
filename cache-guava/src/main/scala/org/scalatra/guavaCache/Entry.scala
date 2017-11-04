package org.scalatra.guavaCache

import java.time._

case class Entry[+A](value: A, expiresAt: Option[LocalDateTime]) {
  def isExpired: Boolean = expiresAt.exists(_.isBefore(LocalDateTime.now))
}
