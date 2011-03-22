package org.scalatra.util

import scala.collection.immutable.Map

trait MultiMapHeadView[A, B] extends Map[A, B] {
  protected def multiMap: Map[A, Seq[B]]

  override def get(key: A) = multiMap.get(key) flatMap { _.headOption }
  override def size = multiMap.size
  override def iterator = multiMap map { case(k, v) => (k, v.head) } iterator
  override def -(key: A) = Map() ++ this - key
  override def +[B1 >: B](kv: (A, B1)) = Map() ++ this + kv
}

object MultiMap {
  def apply() = new MultiMap
  def apply[SeqType <: Seq[String]](wrapped: Map[String, SeqType]) = new MultiMap(wrapped)
}
class MultiMap(wrapped: Map[String, Seq[String]] = Map.empty) extends Map[String, Seq[String]] {

  def get(key: String): Option[Seq[String]] = {
    (wrapped.get(key) orElse wrapped.get(key + "[]"))
  }

  def get(key: Symbol): Option[Seq[String]] = get(key.name)
  def +[B1 >: Seq[String]](kv: (String, B1)) = new MultiMap(wrapped + kv.asInstanceOf[(String, Seq[String])])

  def -(key: String) = new MultiMap(wrapped - key)

  def iterator = wrapped.iterator


}