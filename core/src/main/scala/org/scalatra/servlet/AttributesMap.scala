package org.scalatra
package servlet

import org.scalatra.util.conversion.TypeConverter

import scala.jdk.CollectionConverters._
import Attributes._

/**
 * Adapts for handling servlet objects (e.g., ServletRequest, HttpSession,
 * ServletContext) as mutable map.
 */
trait AttributesMap {

  protected[this] type A
  protected[this] def attributes: A
  protected[this] implicit def attributesTypeClass: Attributes[A]

  /**
   * Optionally returns the attribute associated with the key
   *
   * @param key The key to find
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
   * Returns the attribute associated with the key or default value
   *
   * @param key The key to find
   * @param default The default value, it will be returned when the key does not exist
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or the default value if the key doesn't exist
   */
  def getOrElse(key: String, default: => Any): Any = get(key) getOrElse default

  /**
   * Returns the attribute associated with the key or update attributes with the specified value
   *
   * @param key The key to find
   * @param value The value that will be updated the attribute associated with the key when the key does not exist
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or the updated value if the key doesn't exist
   */
  def getOrElseUpdate(key: String, value: => Any): Any = get(key) match {
    case Some(v) => v
    case None => val u = value; update(key, u); u
  }

  /**
   * Returns the attribute associated with the key or throw an exception when nothing found
   *
   * @param key The key to find
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or throw an exception if the key doesn't exist
   */
  def apply(key: String): Any =
    get(key) getOrElse (throw new ScalatraException(s"Key ${key} not found"))

  /**
   * Updates the attribute associated with the key
   *
   * @param key The key to update
   * @param value The value to update
   */
  def update(key: String, value: Any): Unit = {
    if (attributes != null)
      attributes.setAttribute(key, value.asInstanceOf[AnyRef])
  }

  /**
   * Returns whether the specified key exists
   * @return whether the specified key exists
   */
  def contains(key: String): Boolean = get(key) match {
    case Some(_) => true
    case None => false
  }

  /**
   * Optionally returns and type cast the attribute associated with the key
   *
   * @param key The key to find
   * @tparam T The type of the value
   * @return an option value containing the attributed associated with the key in the underlying servlet object,
   *         or None if none exists
   */
  def getAs[T](key: String)(implicit converter: TypeConverter[Any, T]): Option[T] = {
    get(key) flatMap (converter(_))
  }

  /**
   * Returns the attribute associated with the key or throw an exception when nothing found
   *
   * @param key The key to find
   * @tparam T The type of the value
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or throw an exception if the key doesn't exist
   */
  def as[T](key: String)(implicit converter: TypeConverter[Any, T]): T = {
    getAs[T](key) getOrElse (throw new ScalatraException(s"Key ${key} not found"))
  }

  /**
   * Returns the attribute associated with the key or default value
   *
   * @param key The key to find
   * @param default The default value, it will be returned when the key does not exist
   * @tparam T The type of the value
   * @return an value for the attributed associated with the key in the underlying servlet object,
   *         or the default value if the key doesn't exist
   */
  def getAsOrElse[T](key: String, default: => T)(implicit converter: TypeConverter[Any, T]): T = {
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
   * Applies a function f to add attribute elements
   */
  def foreach[U](f: ((String, Any)) => U): Unit = {
    attributes.getAttributeNames().asScala foreach { name =>
      f((name, attributes.getAttribute(name)))
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

  /**
   * Returns an attributes keys
   */
  def keys: Iterator[String] = attributes.getAttributeNames().asScala

  /**
   * dumps all keys and values
   *
   */
  def dumpAll: String = keys.map {
    a => s"${a}=${attributes.getAttribute(a)}"
  } mkString "&"
}
