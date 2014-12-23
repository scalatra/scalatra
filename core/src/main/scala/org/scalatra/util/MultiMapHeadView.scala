package org.scalatra.util

import scala.collection.immutable.Map

object MultiMapHeadView {
  def empty[A, B]: MultiMapHeadView[A, B] =
    new MultiMapHeadView[A, B] { protected val multiMap = Map.empty[A, Seq[B]] }

  def emptyIndifferent[B]: MultiMapHeadView[String, B] with MapWithIndifferentAccess[B] =
    new MultiMapHeadView[String, B] with MapWithIndifferentAccess[B] { protected val multiMap = Map.empty[String, Seq[B]] }
}
trait MultiMapHeadView[A, B] extends Map[A, B] {
  protected def multiMap: Map[A, Seq[B]]

  override def get(key: A) = multiMap.get(key) flatMap { _.headOption }
  override def size = multiMap.size
  override def iterator = multiMap.map { case (k, v) => (k, v.head) }.iterator
  override def -(key: A) = Map() ++ this - key
  override def +[B1 >: B](kv: (A, B1)) = Map() ++ this + kv
}

