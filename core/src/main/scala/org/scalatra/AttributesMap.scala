package org.scalatra

import scala.collection.mutable.Map
import scala.collection.JavaConversions._
import java.util.Enumeration
import util.MutableMapWithIndifferentAccess

/**
 * Adapts attributes from the Servlet API to a standard Scala mutable map.
 */
trait AttributesMap extends Map[String, AnyRef] with MutableMapWithIndifferentAccess[AnyRef] {
  protected def attributes: Attributes

  def get(key:String) = attributes.getAttribute(key) match {
    case null => None
    case v: AnyRef => Some(v)
  }

  def iterator = attributes.getAttributeNames.asInstanceOf[Enumeration[String]] map { key =>
    (key, attributes.getAttribute(key))
  }

  def +=(kv: (String, AnyRef)) = {
    attributes.setAttribute(kv._1, kv._2)
    this
  }

  def -=(key: String) = {
    attributes.removeAttribute(key)
    this
  }
}