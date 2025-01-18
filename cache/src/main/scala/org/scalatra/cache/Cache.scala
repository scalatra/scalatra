package org.scalatra.cache

import java.time.*

trait Cache {
  def get[V](key: String): Option[V]

  def put[V](key: String, value: V, ttl: Option[Duration]): V

  def remove(key: String): Unit

  def flush(): Unit
}
