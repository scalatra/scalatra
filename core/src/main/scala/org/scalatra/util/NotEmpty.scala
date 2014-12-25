package org.scalatra.util

/**
 * Extractor object, useful for handling empty form parameter submissions:
 *
 * params.get("foo") match {
 *   case NotEmpty(value) => processValue(value)
 *   case _ => message("foo is required")
 * }
 */
object NotEmpty {

  def unapply(s: String): Option[String] = {
    if (s != null && !s.isEmpty) Some(s)
    else None
  }

  def unapply(o: Option[String]): Option[String] = {
    o flatMap { s => unapply(s) }
  }

}
