package scalatra.i18n 

import java.util.Locale
import java.util.ResourceBundle
import java.util.MissingResourceException

class Messages(locale: Locale) {
  private val bundle = ResourceBundle.getBundle("i18n/messages", locale)
  
  def this() = this(Locale.getDefault)
  
  /**
   * Null-safe implementation is preferred by using Option. The caller can 
   * support default value by using getOrElse:
   * messages.get("hello").getOrElse("world")
   * 
   * The return value can also be used with format:
   * messages.get("hello %d").foreach(_.format(5))
   * 
   * To return the string itself:
   * messages.get("hello").get
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
