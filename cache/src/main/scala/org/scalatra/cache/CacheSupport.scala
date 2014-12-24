package org.scalatra.cache

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.ScalatraBase

import scala.concurrent.duration.Duration

trait CacheSupport { self: ScalatraBase =>
  implicit val cacheBackend: Cache
  implicit val keyStrategy: KeyStrategy = DefaultKeyStrategy
  implicit val headerStrategy: HeaderStrategy = DefaultHeaderStrategy

  def cache[A](key: String, ttl: Option[Duration])(value: => A): A = {
    cacheBackend.get[A](key) match {
      case Some(v) => v
      case None => cacheBackend.put(key, value, ttl)
    }
  }

  def cached[A](ttl: Option[Duration])(result: => A)(implicit keyStrategy: KeyStrategy,
    headerStrategy: HeaderStrategy,
    request: HttpServletRequest,
    response: HttpServletResponse): A = {

    val key = keyStrategy.key

    cacheBackend.get[(String, A)](key) match {
      case Some(v) =>
        if (headerStrategy.isUnchanged(v._1)) halt(304)
        else {
          headerStrategy.setRevision(v._1)
          v._2
        }
      case None =>
        val res = result
        val rev = headerStrategy.getNewRevision()
        cacheBackend.put(key, (rev, result), ttl)
        headerStrategy.setRevision(rev)
        res
    }
  }
}
