package org.scalatra

import scala.util.matching.Regex
import util.MultiMap

trait RouteMatcher {

  def apply(): Option[ScalatraKernel.MultiParams]
}

trait ReversibleRouteMatcher {

  def reverse(params: Map[String, String], splats: List[String]): String
}

final class SinatraRouteMatcher(path: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher {

  def apply() = SinatraPathPatternParser(path)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    replaceSplats(
      replaceNamedParams(
        replaceOptionalParams(path, params),
        params
      ), splats)

  private def replaceOptionalParams(slug: String, params: Map[String, String]): String =
    """[\./]\?:[^/?#\.]+\?""".r replaceAllIn (path, s =>
      params.get(s.matched slice (3, s.matched.size - 1)) match {
        case Some(value) => s.matched.head + value
        case None => ""
      })

  private def replaceNamedParams(slug: String, params: Map[String, String]): String =
    """:[^/?#\.]+""".r replaceAllIn (slug, s => {
      val param = s.matched.tail
      params.get(param) match {
        case Some(value) => value
        case None => throw new Exception(
          "The url \"%s\" requires param \"%s\"" format (path, param))
    }})

  private def replaceSplats(slug: String, splats: List[String]): String =
    splats match {
      case Nil => slug
      case s :: rest => replaceSplats("""\*""".r replaceFirstIn (slug, s), rest)
    }

  override def toString = path
}

final class RailsRouteMatcher(path: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher {

  def apply() = RailsPathPatternParser(path)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    dynamicSegments(optionalSegments(path, params), params)

  private def optionalSegments(slug: String, params: Map[String, String]): String =
    """([^\\])\(([^\)]*[^\\])\)""".r replaceAllIn (slug, s => s.group(1) +
      (try dynamicSegments(s.group(2), params) catch { case e: Exception => "" })
    )

  private def dynamicSegments(slug: String, params: Map[String, String]): String =
    """(.?)(:([a-z_][^/?#\.]+))""".r replaceAllIn (slug, s => {
      if (s.group(1) == "\\") s.group(2)
      else params.get(s.group(3)) match {
        case Some(value) => s.group(1) + value
        case None => throw new Exception(
          "The url \"%s\" requires param \"%s\"" format (path, s.group(3))
        )
    }})
}

final class PathPatternRouteMatcher(pattern: PathPattern, requestPath: => String)
  extends RouteMatcher {

  def apply() = pattern(requestPath)

  override def toString = pattern.regex.toString
}

final class RegexRouteMatcher(regex: Regex, requestPath: => String)
  extends RouteMatcher {

  def apply() = regex.findFirstMatchIn(requestPath) map { _.subgroups match {
    case Nil => MultiMap()
    case xs => Map("captures" -> xs)
  }}

  override def toString = regex.toString
}

final class BooleanBlockRouteMatcher(block: => Boolean) extends RouteMatcher {

  def apply() = if (block) Some(MultiMap()) else None

  override def toString = "[Boolean Guard]"
}
