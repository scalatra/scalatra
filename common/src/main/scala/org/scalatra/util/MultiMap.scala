package org.scalatra.util

import scala.collection.immutable.Map

object MultiMap {
  def apply() = new MultiMap
  def apply[SeqType <: Seq[String]](wrapped: Map[String, SeqType]) = new MultiMap(wrapped)

  def empty = apply()
  implicit def map2MultiMap(map: Map[String, Seq[String]]) = new MultiMap(map)
}

class MultiMap(wrapped: Map[String, Seq[String]] = Map.empty) extends Map[String, Seq[String]] {

  def get(key: String): Option[Seq[String]] = {
    (wrapped.get(key) orElse wrapped.get(key + "[]"))
  }

  def get(key: Symbol): Option[Seq[String]] = get(key.name)
  def +[B1 >: Seq[String]](kv: (String, B1)) = new MultiMap(wrapped + kv.asInstanceOf[(String, Seq[String])])

  def -(key: String) = new MultiMap(wrapped - key)

  def iterator = wrapped.iterator

  override def default(a: String): Seq[String] = wrapped.default(a)
}
