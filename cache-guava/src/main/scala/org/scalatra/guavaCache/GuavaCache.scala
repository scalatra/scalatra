package org.scalatra.guavaCache

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import org.scalatra.cache.Cache

class GuavaCache(ttlMs: Long) extends Cache {
  lazy private[this] val cache = CacheBuilder.newBuilder()
    .expireAfterWrite(ttlMs, TimeUnit.MILLISECONDS)
    .maximumSize(10000L)
    .build[String, Object]

  override def get[V](key: String): Option[V] = Option(cache.getIfPresent(key).asInstanceOf[V])

  override def put[V](key: String, value: V): V = {
    cache.put(key, value.asInstanceOf[Object])
    value
  }

  override def remove(key: String): Unit = cache.invalidate(key)

  override def flush(): Unit = cache.invalidateAll()
}
