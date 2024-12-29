package org.scalatra.util

object MultiMapHeadView {

  def empty[A, B]: MultiMapHeadView[A, B] = {
    new MultiMapHeadView[A, B] {
      override protected val multiMap = Map.empty[A, Seq[B]]
    }
  }

}

trait MultiMapHeadView[A, B] {

  protected def multiMap: Map[A, Seq[B]]

  def apply(key: A): B = multiMap.get(key) match {
    case Some(v) => v.head
    case None    => throw new NoSuchElementException(s"Key ${key} not found")
  }

  def get(key: A): Option[B] = multiMap.get(key) flatMap { _.headOption }

  def getOrElse(key: A, default: => B): B = toMap.getOrElse(key, default)

  def size: Int = multiMap.size

  def foreach[U](f: ((A, B)) => U): Unit = multiMap foreach { case (k, v) => f((k, v.head)) }

  def iterator: Iterator[(A, B)] = multiMap.iterator.flatMap { case (k, v) =>
    v.headOption.map { _v => (k, _v) }
  }

  def toMap: Map[A, B] = multiMap map { case (k, v) => (k -> v.head) }

  def -(key: A): Map[A, B] = toMap - key

  def +[B1 >: B](kv: (A, B1)): Map[A, B1] = toMap + kv

  def isDefinedAt(key: A): Boolean = get(key) match {
    case Some(_) => true
    case None    => false
  }

  def contains(key: A): Boolean = get(key) match {
    case Some(_) => true
    case None    => false
  }

}
