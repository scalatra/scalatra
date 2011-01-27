package org.scalatra.pattern

/**
 * Parses a string into a path pattern for routing.
 */
trait PathPatternParser {
  def apply(pattern: String): PathPattern
}
