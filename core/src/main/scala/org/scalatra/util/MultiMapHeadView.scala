package org.scalatra.util

import scala.collection.immutable.Map

object MultiMapHeadView {

  def empty[A, B]: MultiMapHeadView[A, B] = {
    new MultiMapHeadView[A, B] {
      override protected val multiMap = Map.empty[A, Seq[B]]
    }
  }

  def emptyIndifferent[B]: MultiMapHeadView[String, B] with MapWithIndifferentAccess[B] = {
    new MultiMapHeadView[String, B] with MapWithIndifferentAccess[B] {
      override protected val multiMap = Map.empty[String, Seq[B]]
    }
  }

}

trait MultiMapHeadView[A, B] extends Map[A, B] {

  protected def multiMap: Map[A, Seq[B]]

  override def get(key: A): Option[B] = multiMap.get(key) flatMap { _.headOption }

  override def size: Int = multiMap.size

  override def iterator: Iterator[(A, B)] = multiMap.map { case (k, v) => (k, v.head) }.iterator

  override def -(key: A): Map[A, B] = Map() ++ this - key

  override def +[B1 >: B](kv: (A, B1)): Map[A, B1] = Map() ++ this + kv

}

