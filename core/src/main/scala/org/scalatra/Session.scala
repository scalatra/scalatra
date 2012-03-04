package org.scalatra

import scala.collection.mutable

trait Session extends mutable.Map[String, AnyRef] {
  def id: String

  def invalidate(): Unit
}
