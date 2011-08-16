package org.scalatra

import scala.util.matching.Regex
import util.MultiMap

class RouteMatcher(
  val matcher: () => Option[ScalatraKernel.MultiParams],
  val pattern: String
) {

  def apply(): Option[ScalatraKernel.MultiParams] = matcher()

  override def toString: String = pattern
}

object RouteMatcher {

  def apply(matcher: () => Option[ScalatraKernel.MultiParams], pattern: String): RouteMatcher =
    new RouteMatcher(matcher, pattern)

  def stringPath(path: String, requestPath: () => String): RouteMatcher =
    apply(() => SinatraPathPatternParser(path)(requestPath()), path)

  def pathPattern(pattern: PathPattern, requestPath: () => String): RouteMatcher =
    apply(() => pattern(requestPath()), pattern.regex.toString)

  def regex(regex: Regex, requestPath: () => String): RouteMatcher =
    apply(() => regex.findFirstMatchIn(requestPath()) map { _.subgroups match {
        case Nil => MultiMap()
        case xs => Map("captures" -> xs)
      }},
      regex.toString)

  def booleanBlock(block: => Boolean): RouteMatcher =
    apply(() => { if (block) Some(MultiMap()) else None }, "[Boolean Guard]")
}
