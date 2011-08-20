package org.scalatra

import scala.util.matching.Regex
import util.MultiMap

trait RouteMatcher
{
  def apply(): Option[ScalatraKernel.MultiParams]
}

trait ReversibleRouteMatcher
{
  def reverse(params: Map[String, String], splats: List[String]): String
}

final class SinatraRouteMatcher(path: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher
{
  def apply() = SinatraPathPatternParser(path)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    replaceSplats(replaceNamedParams(params), splats)

  private def replaceNamedParams(params: Map[String, String]) =
    """:[^/?#\.]+""".r replaceAllIn (path, s =>
      params.get(s.toString.tail) match {
        case Some(value) => value
        case None => throw new Exception("The url \"%s\" requires param \"%s\"" format (path, s))
      })

  private def replaceSplats(slug: String, splats: List[String]): String =
    splats match {
      case Nil => slug
      case s :: rest => replaceSplats("""\*""".r replaceFirstIn (slug, s), rest)
    }

  override def toString = path
}

final class PathPatternRouteMatcher(pattern: PathPattern, requestPath: => String)
  extends RouteMatcher
{
  def apply() = pattern(requestPath)

  override def toString = pattern.regex.toString
}

final class RegexRouteMatcher(regex: Regex, requestPath: => String)
  extends RouteMatcher
{
  def apply() = regex.findFirstMatchIn(requestPath) map { _.subgroups match {
    case Nil => MultiMap()
    case xs => Map("captures" -> xs)
  }}

  override def toString = regex.toString
}

final class BooleanBlockRouteMatcher(block: => Boolean) extends RouteMatcher
{
  def apply() = if (block) Some(MultiMap()) else None

  override def toString = "[Boolean Guard]"
}
