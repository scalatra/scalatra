package org.scalatra.cache

trait CacheSupport {
  implicit val cacheBackend: Cache

  def cache[A](key: String, ttlMs: Long = 60000)(f: => A): A = {
    cacheBackend.get[A](key) match {
      case Some(v)  => v
      case None     => cacheBackend.put(key, f, ttlMs)
    }
  }
}
