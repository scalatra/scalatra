package org.scalatra.util

import scala.collection.mutable.Map

/**
 * @see MapWithIndifferentAccess
 */

@deprecated("MutableMapWithIndifferentAccess is deprecated from Scalatra 2.7.0. It will be deleted in the next major version. Please unify the key type of Map to either String or Symbol.", "2.7.0")
trait MutableMapWithIndifferentAccess[B]
  extends MapWithIndifferentAccess[B]
  with Map[String, B] {

  def update(key: Symbol, value: B): Unit = {
    update(key.name, value)
  }

}
