package org.scalatra.cache

trait Cache {
  def get[V](key: String): Option[V]

  def put[V](key: String, value: V): V

  def remove(key: String)

  def flush()
}
