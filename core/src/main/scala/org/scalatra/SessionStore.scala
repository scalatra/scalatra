package org.scalatra

import collection.mutable

trait SessionStore[SessionType <: HttpSession] extends mutable.Map[String, SessionType] with mutable.MapLike[String,  SessionType, SessionStore[SessionType]] {

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


