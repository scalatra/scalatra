package org.scalatra

import scala.collection.mutable.Map
import scala.collection.JavaConversions._
import java.util.Enumeration
import util.MutableMapWithIndifferentAccess

/**
 * Adapts attributes from servlet objects (e.g., ServletRequest, HttpSession,
 * ServletContext) to a mutable map.
 */
trait AttributesMap extends Map[String, AnyRef] with MutableMapWithIndifferentAccess[AnyRef] {
  protected def attributes: Attributes

  /**
   * Optionally returns the attribute associated with the key
   *
   * @return an option value containing the attribute associated with the key
   * in the underlying servlet object, or None if none exists.
   */
  def get(key:String): Option[AnyRef] = attributes.getAttribute(key) match {
    case null => None
    case v: AnyRef => Some(v)
  }

  /**
   * Creates a new iterator over all attributes in the underlying servlet object.
   *
   * @return the new iterator
   */
  def iterator: Iterator[(String, AnyRef)] =
    attributes.getAttributeNames.asInstanceOf[Enumeration[String]] map { key =>
      (key, attributes.getAttribute(key))
    }

  /**
   * Sets an attribute on the underlying servlet object.
   *
   * @param kv the key/value pair.  If the value is null, has the same effect
   * as calling `-=(kv._1)`.
   *
   * @return the map itself
   */
  def +=(kv: (String, AnyRef)) = {
    attributes.setAttribute(kv._1, kv._2)
    this
  }

  /**
   * Removes an attribute from the underlying servlet object.
   *
   * @param key the key to remove
   *
   * @return the map itself
   */
  def -=(key: String) = {
    attributes.removeAttribute(key)
    this
  }
}
