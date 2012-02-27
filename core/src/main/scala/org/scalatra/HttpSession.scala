package org.scalatra

import scala.collection.mutable

trait HttpSession extends mutable.Map[String, AnyRef] {
  def id: String

  def invalidate(): Unit
}
