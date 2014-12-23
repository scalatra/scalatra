package org.scalatra
package i18n

import java.util.{ Locale, MissingResourceException, ResourceBundle }

object Messages {
  def apply(locale: Locale = Locale.getDefault, bundlePath: String = "i18n/messages"): Messages = new Messages(locale, bundlePath)
}
class Messages(locale: Locale, bundlePath: String = "i18n/messages") {
  private[this] val bundle = ResourceBundle.getBundle(bundlePath, locale)

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
   *
   * @param key The key to find the message for
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
   * Return the value for the key or fall back to the provided default
   */
  def getOrElse(key: String, default: => String): String = try {
    bundle.getString(key)
  } catch {
    case e: MissingResourceException => default
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
