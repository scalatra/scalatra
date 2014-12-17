package org.scalatra.cache

trait Cache {
  def get[V](key: String): Option[V]

  def put[V](key: String, value: V, ttlMs: Long): V

  def remove(key: String)
}
