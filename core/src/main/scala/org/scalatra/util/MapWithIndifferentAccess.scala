package org.scalatra.util

import scala.collection.Map

/**
 * Inspired by Rails' MapWithIndifferentAccess, allows the substitution of symbols for strings as map keys.  Note
 * that the map is still keyed with strings; symbols are stored in permgen, so symbol keys maps should not be used
 * for maps with arbitrary keys.  There is no performance gain using symbols.  It is here to make our Rubyists feel
 * more at home.
 */

@deprecated("MapWithIndifferentAccess is deprecated from Scalatra 2.7.0. It will be deleted in the next major version. Please unify the key type of Map to either String or Symbol.", "2.7.0")
trait MapWithIndifferentAccess[+B] extends Map[String, B] {

  def get(key: Symbol): Option[B] = get(key.name)

  def getOrElse[B1 >: B](key: Symbol, default: => B1): B1 = getOrElse(key.name, default)

  def apply(key: Symbol): B = apply(key.name)

}

