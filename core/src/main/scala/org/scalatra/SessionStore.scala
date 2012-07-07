package org.scalatra

import collection.mutable
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._

trait SessionStore[SessionType <: HttpSession] extends mutable.Map[String, SessionType] with mutable.MapLike[String,  SessionType, SessionStore[SessionType]] with Initializable {


  @volatile protected var appContext: AppContext = null
  /**
   * A hook to initialize the class with some configuration after it has
   * been constructed.
   */
  def initialize(config: AppContext) {
    if (appContext != config) appContext = config
  }

  protected def meta: HttpSessionMeta[SessionType]
  def newSession: SessionType

  def newSessionWithId(id: String): SessionType

  def stop()
  def invalidateAll()

  def invalidate(session: HttpSession): this.type = invalidate(session.id)
  def invalidate(sessionId: String): this.type

  override def empty: this.type = emptyStore.asInstanceOf[this.type]
  protected def emptyStore: this.type
}


class NoopHttpSession extends HttpSession {
  protected def self: mutable.ConcurrentMap[String, Any] = new ConcurrentHashMap[String, Any]().asScala

  def id: String = GenerateId()
}
object NoopHttpSession extends HttpSessionMeta[NoopHttpSession] {
  def empty: NoopHttpSession = new NoopHttpSession

  def emptyWithId(sessionId: String): NoopHttpSession = new NoopHttpSession
}
class NoopSessionStore extends SessionStore[NoopHttpSession] {
  def +=(kv: (String, NoopHttpSession)) = this

  def -=(key: String) = this

  protected def meta: HttpSessionMeta[NoopHttpSession] = NoopHttpSession

  def newSession: NoopHttpSession = meta.empty

  def newSessionWithId(id: String): NoopHttpSession = meta.emptyWithId(id)

  def stop() {}

  def invalidateAll() {}

  def invalidate(sessionId: String) = this.asInstanceOf[this.type]

  protected def emptyStore = new NoopSessionStore().asInstanceOf[this.type]

  def get(key: String): Option[NoopHttpSession] = None

  def iterator: Iterator[(String, NoopHttpSession)] = Iterator.empty
}
