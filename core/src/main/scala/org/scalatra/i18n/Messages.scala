package org.scalatra
package i18n 

import java.util.Locale
import java.util.ResourceBundle
import java.util.MissingResourceException

object Messages {
  def apply(locale: Locale = Locale.getDefault): Messages = new Messages(locale)
}
class Messages(locale: Locale) {
  private val bundle = ResourceBundle.getBundle("i18n/messages", locale)

  /**
   * Null-safe implementation is preferred by using Option. The caller can 
   * support default value by using getOrElse:
   * messages.get("hello").getOrElse("world")
   * 
   * The return value can also be used with format:
   * messages.get("hello %d").map(_.format(5))
   * 
   * To return the string itself:
   * messages("hello")
   */
  def get(key: String): Option[String] = {
    try {
      Some(bundle.getString(key))
    } catch {
      case e: MissingResourceException => None
    }
  }
  
  def apply(key: String): String = {
    bundle.getString(key)
  }
  
  /**
   * Returned the value for the key or the default
   */
  def apply(key: String, default: String): String = {
    try {
      bundle.getString(key)
    } catch {
      case e: MissingResourceException => default
    }
  }
}
