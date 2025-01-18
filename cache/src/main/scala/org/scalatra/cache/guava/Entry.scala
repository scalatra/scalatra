package org.scalatra.cache.guava

import java.time.*

case class Entry[+A](value: A, expiresAt: Option[LocalDateTime]) {
  def isExpired: Boolean = expiresAt.exists(_.isBefore(LocalDateTime.now))
}
