package org.scalatra
package servlet

import org.scalatra.util.MutableMapWithIndifferentAccess
import org.scalatra.util.conversion.TypeConverter

import scala.collection.JavaConverters._
import scala.collection.mutable.Map

/**
 * Adapts attributes from servlet objects (e.g., ServletRequest, HttpSession,
 * ServletContext) to a mutable map.
 */
trait AttributesMap extends Map[String, Any] with MutableMapWithIndifferentAccess[Any] {

  protected def attributes: Attributes

  /**
   * Optionally returns the attribute associated with the key
   *
   * @return an option value containing the attribute associated with the key
   * in the underlying servlet object, or None if none exists.
   */
  def get(key: String): Option[Any] = {
    if (attributes == null) None
    else {
      attributes.getAttribute(key) match {
        case null => None
        case v => Some(v)
      }
    }
  }

  /**
   * Optionally return and type cast the attribute associated with the key
   *
   * @param key The key to find
   * @tparam T The type of the value
   * @return an option value containing the attributed associated with the key in the underlying servlet object,
   *         or None if none exists
   */
  def getAs[T](key: String)(implicit mf: Manifest[T], converter: TypeConverter[Any, T]): Option[T] = {
    get(key) flatMap (converter(_))
  }

  /**
   * Return the attribute associated with the key or throw an exception when nothing found
   *
   * @param key The key to find
   * @tparam T The type of the value
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or throw an exception if the key doesn't exist
   */
  def as[T](key: String)(implicit mf: Manifest[T], converter: TypeConverter[Any, T]): T = {
    getAs[T](key) getOrElse (throw new ScalatraException("Key " + key + " not found"))
  }

  /**
   * Return the attribute associated with the key or throw an exception when nothing found
   *
   * @param key The key to find
   * @tparam T The type of the value
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or throw an exception if the key doesn't exist
   */
  def getAsOrElse[T](key: String, default: => T)(
    implicit mf: Manifest[T], converter: TypeConverter[Any, T]): T = {
    getAs[T](key) getOrElse default
  }

  /**
   * Creates a new iterator over all attributes in the underlying servlet object.
   *
   * @return the new iterator
   */
  def iterator: Iterator[(String, Any)] = {
    attributes.getAttributeNames().asScala map { key =>
      (key, attributes.getAttribute(key))
    }
  }

  /**
   * Sets an attribute on the underlying servlet object.
   *
   * @param kv the key/value pair.  If the value is null, has the same effect
   * as calling `-=(kv._1)`.
   *
   * @return the map itself
   */
  def +=(kv: (String, Any)): AttributesMap.this.type = {
    attributes.setAttribute(kv._1, kv._2.asInstanceOf[AnyRef])
    this
  }

  /**
   * Removes an attribute from the underlying servlet object.
   *
   * @param key the key to remove
   *
   * @return the map itself
   */
  def -=(key: String): AttributesMap.this.type = {
    attributes.removeAttribute(key)
    this
  }

}
