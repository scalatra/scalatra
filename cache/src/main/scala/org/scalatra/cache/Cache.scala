package org.scalatra.cache

import scala.concurrent.duration.Duration

trait Cache {
  def get[V](key: String): Option[V]

  def put[V](key: String, value: V, ttl: Option[Duration]): V

  def remove(key: String)

  def flush()
}
