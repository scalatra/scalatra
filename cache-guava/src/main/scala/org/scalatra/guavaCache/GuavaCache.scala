package org.scalatra.guavaCache

import com.google.common.cache.CacheBuilder
import org.scalatra.cache.Cache
import java.time._

object GuavaCache extends Cache {
  private[this] val cache = CacheBuilder.newBuilder()
    .maximumSize(10000L)
    .build[String, Object]

  override def get[V](key: String): Option[V] = Option(cache.getIfPresent(key).asInstanceOf[Entry[V]])
    .flatMap(e =>
      if (e.isExpired) {
        remove(key)
        None
      } else {
        Some(e.value)
      })

  override def put[V](key: String, value: V, ttl: Option[Duration]): V = {
    cache.put(key, new Entry(value.asInstanceOf[Object], ttl.map(t => LocalDateTime.now.plus(t))))
    value
  }

  override def remove(key: String): Unit = cache.invalidate(key)

  override def flush(): Unit = cache.invalidateAll()
}
