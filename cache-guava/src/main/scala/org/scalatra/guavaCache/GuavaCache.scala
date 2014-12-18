package org.scalatra.guavaCache

import com.google.common.cache.CacheBuilder
import org.scalatra.cache.Cache

class GuavaCache extends Cache {
  lazy val cache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Object]

  override def get[V](key: String): Option[V] = Option(cache.getIfPresent(key).asInstanceOf[V])

  override def put[V](key: String, value: V, ttlMs: Long): V = {
    cache.put(key, value.asInstanceOf[Object])
    value
  }

  override def remove(key: String): Unit = cache.invalidate(key)

  override def flush(): Unit = cache.invalidateAll()
}
