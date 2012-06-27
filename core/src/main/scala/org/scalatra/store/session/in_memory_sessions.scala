package org.scalatra
package store
package session

import _root_.akka.util.Duration
import java.util.concurrent.atomic.AtomicLong
import collection.mutable
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._
import _root_.akka.actor.Cancellable
import _root_.akka.util.duration._


object InMemorySession extends HttpSessionMeta[InMemorySession] {
  def empty = new InMemorySession(new ConcurrentHashMap[String, Any].asScala)

  def emptyWithId(sessionId: String) = new InMemorySession(new ConcurrentHashMap[String, Any].asScala, sessionId)
}
class InMemorySession(protected val self: mutable.ConcurrentMap[String, Any], val id: String = GenerateId()) extends HttpSession {
  override protected def newSession(newSelf: mutable.ConcurrentMap[String, Any]): InMemorySession =
    new InMemorySession(newSelf, id)
}

object InMemorySessionStore {

  private case class Entry[T](value: T, expiration: Duration) {
    private val lastAccessed = new AtomicLong(System.currentTimeMillis)
    def isExpired  = lastAccessed.get < (System.currentTimeMillis - expiration.toMillis)
    def expire() = lastAccessed.set(0L)
    def tickAccess() = lastAccessed.set(System.currentTimeMillis)
  }

}

/**
 * crude and naive non-blocking LRU implementation that expires sessions after the specified timeout
 * @param appContext
 */
class InMemorySessionStore(implicit appContext: AppContext) extends SessionStore[InMemorySession] with mutable.MapLike[String,  InMemorySession, InMemorySessionStore] {


  import InMemorySessionStore.Entry
  protected val meta = InMemorySession

  private[this] var timeout = expireSessions

  private[this] val self: mutable.ConcurrentMap[String, Entry[InMemorySession]] =
    new ConcurrentHashMap[String, Entry[InMemorySession]]().asScala


  def emptyStore = new InMemorySessionStore().asInstanceOf[this.type]

  override def seq = self map { case (k, v) => (k -> v.value) }

  def get(key: String) = {
    self get key filterNot (_.isExpired) map (_.value)
  }

  def newSession = {
    val sess = meta.empty
    self += sess.id -> Entry(sess, appContext.sessionTimeout)
    sess
  }

  def newSessionWithId(id: String) = {
    val sess = meta.emptyWithId(id)
    self += sess.id -> Entry(sess, appContext.sessionTimeout)
    sess
  }


  def invalidate(sessionId: String) = this -= sessionId

  def invalidateAll() = self.clear()

  def iterator = {
    val o = self.iterator filterNot (_._2.isExpired)
    new Iterator[(String, InMemorySession)] {
      def hasNext = o.hasNext

      def next() = {
        val nxt = o.next()
        nxt._2.tickAccess()
        (nxt._1, nxt._2.value)
      }
    }
  }

  private[this] def expireSessions: Cancellable = appContext.actorSystem.scheduler.scheduleOnce(1 second) {
    self.valuesIterator filter (_.isExpired) foreach { self -= _.value.id }
    InMemorySessionStore.this.timeout = expireSessions
  }

  def +=(kv: (String, InMemorySession)) = {
    self += kv._1 -> Entry(kv._2, appContext.sessionTimeout)
    this
  }

  def -=(key: String) = {
    self -= key
    this
  }

  def stop() {
    if (timeout != null && !timeout.isCancelled) timeout.cancel()
  }
}