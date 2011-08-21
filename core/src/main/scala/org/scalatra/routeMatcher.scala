package org.scalatra

import scala.util.matching.Regex
import scala.util.parsing.combinator._
import util.MultiMap

trait RouteMatcher {

  def apply(): Option[ScalatraKernel.MultiParams]
}

trait ReversibleRouteMatcher {

  def reverse(params: Map[String, String], splats: List[String]): String
}

final class SinatraRouteMatcher(pattern: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher {

  lazy val generator: (Url => Url) = GeneratorParser(pattern)

  def apply() = SinatraPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    generator(Url("", params, splats)).path

  case class Url(path: String, params: Map[String, String], splats: List[String]) {

    def addLiteral(text: String) = copy(path = path + text)

    def addSplat = copy(path = path + splats.head, splats = splats.tail)

    def addNamed(name: String) =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else throw new Exception("Url \"%s\" requires param \"%s\"" format (pattern, name))

    def addOptional(name: String) =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else this

    def addPrefixedOptional(name: String, prefix: String) =
      if (params contains name) copy(path = path + prefix + params(name), params = params - name)
      else this
  }

  object GeneratorParser extends RegexParsers {

    def apply(pattern: String): (Url => Url) = parseAll(tokens, pattern) get

    private def tokens: Parser[Url => Url] = rep(token) ^^ (_ reduceLeft {
      (acc, fun) => (url: Url) => fun(acc(url))
    })

    private def token: Parser[Url => Url] = splat | prefixedOptional | optional | named | literal

    private def splat: Parser[Url => Url] = "*" ^^^ { url => url addSplat }

    private def prefixedOptional: Parser[Url => Url] =
      ("." | "/") ~ "?:" ~ """\w+""".r ~ "?" ^^ {
        case p ~ "?:" ~ o ~ "?" => url => url addPrefixedOptional (o, p)
      }

    private def optional: Parser[Url => Url] =
      "?:" ~> """\w+""".r <~ "?" ^^ { x => url => url addOptional x }

    private def named: Parser[Url => Url] =
      ":" ~> """\w+""".r ^^ { x => url => url addNamed x }

    private def literal: Parser[Url => Url] =
      ("""[\.\+\(\)\$]""".r | ".".r) ^^ { x => url => url addLiteral x }
  }

  override def toString = pattern
}

final class RailsRouteMatcher(pattern: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher {

  def apply() = RailsPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String = {
    "todo"
  }
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
