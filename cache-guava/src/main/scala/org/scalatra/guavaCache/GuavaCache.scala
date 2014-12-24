package org.scalatra.guavaCache

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import org.joda.time.DateTime
import org.scalatra.cache.Cache

import scala.concurrent.duration.Duration

object GuavaCache extends Cache {
  private[this] val cache = CacheBuilder.newBuilder()
    .maximumSize(10000L)
    .build[String, Object]

  override def get[V](key: String): Option[V] = Option(cache.getIfPresent(key).asInstanceOf[Entry])
    .flatMap(e => if (e.isExpired) None else Some(e.value.asInstanceOf[V]))

  override def put[V](key: String, value: V, ttl: Option[Duration]): V = {
    cache.put(key, new Entry(value.asInstanceOf[Object], ttl.map(t => DateTime.now.plusMillis(t.toMillis.toInt))))
    value
  }

  override def remove(key: String): Unit = cache.invalidate(key)

  override def flush(): Unit = cache.invalidateAll()
}
