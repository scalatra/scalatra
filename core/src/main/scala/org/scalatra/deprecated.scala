package org.scalatra

/**
 * Extractor object, useful for handling empty form parameter submissions:
 *
 * params.get("foo") match {
 *   case NotEmpty(value) => processValue(value)
 *   case _ => message("foo is required")
 * }
 */
@deprecated("Moved to org.scalatra.util.NotEmpty")
object NotEmpty {
  def unapply(s: String) = if (s != null && !s.isEmpty) Some(s) else None
  def unapply(o: Option[String]): Option[String] = o flatMap { s => unapply(s) }
}
