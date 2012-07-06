package org.scalatra
package store
package session

import _root_.akka.util.Duration
import collection.mutable
import java.util.concurrent.TimeUnit
import collection.JavaConverters._
import _root_.akka.util.duration._
import com.google.common.collect.MapMaker
import com.google.common.cache._


object InMemorySession extends HttpSessionMeta[InMemorySession] {
  def empty = new InMemorySession(newMap)

  private[this] def newMap = (new MapMaker).makeMap[String, Any]().asScala
  def emptyWithId(sessionId: String) = new InMemorySession(newMap, sessionId)
}
class InMemorySession(protected val self: mutable.ConcurrentMap[String, Any], val id: String = GenerateId()) extends HttpSession {
  override protected def newSession(newSelf: mutable.ConcurrentMap[String, Any]): InMemorySession =
    new InMemorySession(newSelf, id)
}

/**
 * Google Guava cache based LRU implementation that expires sessions after the specified timeout
 * @param appContext
 */
class InMemorySessionStore(sessionTimeout: Duration = 20.minutes) extends SessionStore[InMemorySession] with mutable.MapLike[String,  InMemorySession, InMemorySessionStore] {

  protected val meta = InMemorySession

  private[this] val self: Cache[String, InMemorySession] = {
    val cb = CacheBuilder.newBuilder()
    cb.expireAfterAccess(sessionTimeout.toMillis, TimeUnit.MILLISECONDS)
    cb.build[String, InMemorySession]()
  }

  def emptyStore = new InMemorySessionStore(sessionTimeout).asInstanceOf[this.type]

  override def seq = self.asMap().asScala

  def get(key: String) = {
    Option(self getIfPresent key)
  }

  def newSession = {
    val sess = meta.empty
    self.put(sess.id, sess)
    sess
  }

  def newSessionWithId(id: String) = {
    val sess = meta.emptyWithId(id)
    self.put(sess.id, sess)
    sess
  }


  def invalidate(sessionId: String) = this -= sessionId

  def invalidateAll() = self.invalidateAll()

  def iterator = {
    self.asMap().asScala.iterator
  }

  def +=(kv: (String, InMemorySession)) = {
    self.put(kv._1, kv._2)
    this
  }

  def -=(key: String) = {
    self.invalidate(key)
    this
  }

  def stop() {
    self.invalidateAll()
    self.cleanUp()
  }
}