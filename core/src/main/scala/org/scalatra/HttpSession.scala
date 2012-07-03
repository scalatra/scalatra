package org.scalatra

import collection.{GenTraversableOnce, mutable}
import collection.JavaConversions._
import java.security.SecureRandom
import mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import util.MapWithIndifferentAccess


trait HttpSessionMeta[SessionType <: HttpSession] {
  def empty: SessionType
  def emptyWithId(sessionId: String): SessionType
}


trait HttpSession extends mutable.Map[String, Any] with mutable.MapLike[String, Any, HttpSession] {
  protected implicit def map2gmap(mmap: scala.collection.Map[String, Any]) = new ConcurrentHashMap[String, Any]() ++= mmap
//  protected implicit def mmap2gmap(mmap: mutable.Map[String, Any]) = new MapMaker().makeMap[String, Any]() ++= mmap

  protected def self: mutable.ConcurrentMap[String, Any]

  override def get(key: String): Option[Any] = self.get(key)

  def get(key: Symbol): Option[Any] = self.get(key.name)

  override def iterator: Iterator[(String, Any)] = self.iterator

  override def isEmpty: Boolean = self.isEmpty

  override def getOrElse[B1 >: Any](key: String, default: => B1): B1 = self.getOrElse(key, default)

  def getOrElse[B1 >: Any](key: Symbol, default: => B1): B1 = self.getOrElse(key.name, default)

  def apply(key: Symbol): Any = self.apply(key.name)

  override def apply(key: String): Any = self.apply(key)

  override def contains(key: String): Boolean = self.contains(key)

  def contains(key: Symbol): Boolean = self.contains(key.name)

  override def isDefinedAt(key: String) = self.isDefinedAt(key)

  def isDefinedAt(key: Symbol) = self.isDefinedAt(key.name)

  override def keySet = self.keySet
  override def keysIterator: Iterator[String] = self.keysIterator
  override def keys: Iterable[String] = self.keys
  override def values: Iterable[Any] = self.values
  override def valuesIterator: Iterator[Any] = self.valuesIterator
  override def default(key: String): Any = self.default(key)
  override def filterKeys(p: String => Boolean): HttpSession = newSession(self.filterKeys(p))
  override def mapValues[C](f: Any => C) = self.mapValues(f)
  override def filterNot(p: ((String, Any)) => Boolean) = newSession(self filterNot p)

  override def addString(Any: StringBuilder, start: String, sep: String, end: String): StringBuilder =
    self.addString(Any, start, sep, end)

  def id: String

  protected def newSession(newSelf: mutable.ConcurrentMap[String, Any]): HttpSession = {
    val cid = id
    new HttpSession {
      val self = newSelf

      val id = cid
    }
  }


  override def repr = this
  override def empty: HttpSession = newSession(HttpSession.this.self.empty)
  override def updated [B1 >: Any](key: String, value: B1): HttpSession = newSession(self.updated(key, value))
  def updated [B1 >: Any](key: Symbol, value: B1) = newSession(self.updated(key.name, value))

  override def - (key: String): HttpSession = newSession(self - key)
  override def + [B1 >: Any] (kv: (String, B1)): HttpSession = newSession(self + kv)
  override def + [B1 >: Any] (elem1: (String, B1), elem2: (String, B1), elems: (String, B1) *): HttpSession =
    newSession(self.+(elem1, elem2, elems: _*))
  override def ++[B1 >: Any](xs: GenTraversableOnce[(String, B1)]): HttpSession = newSession(self ++ xs)

  override def += (kv: (String, Any)) = {
    self += kv
    this
  }
  override def -= (key: String) = {
    self -= key
    this
  }

  def invalidate()(implicit appContext: AppContext) { appContext.sessions.invalidate(this) }

}

