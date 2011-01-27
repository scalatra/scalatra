package org.scalatra
package pattern

import scala.util.parsing.combinator.RegexParsers

trait RegexPathPatternParser extends PathPatternParser with RegexParsers {
  /**
   * This parser gradually builds a regular expression.  Some intermediate
   * strings are not valid regexes, so we wait to compile until the end.
   */
  protected case class PartialPathPattern(
    regex: String,
    captureGroupNames: List[String] = Nil)
  {
    def toPathPattern: PathPattern = PathPattern(regex.r, captureGroupNames)

    def +(other: PartialPathPattern): PartialPathPattern = PartialPathPattern(
      this.regex + other.regex,
      this.captureGroupNames ::: other.captureGroupNames
    )
  }
}
