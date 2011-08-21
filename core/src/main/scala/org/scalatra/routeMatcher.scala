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

  lazy val generator: Generator = GeneratorParser(pattern)

  def apply() = SinatraPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    generator(Url("", params, splats)).path

  case class Url(path: String, params: Map[String, String], splats: List[String]) {

    def add(text: String) = copy(path = path + text)

    def addSplat = copy(path = path + splats.head, splats = splats.tail)
  }

  sealed trait Generator {

    def apply(url: Url): Url

    def +(block: Generator) = new Generator {
      def apply(c: Url) = block(Generator.this(c))
    }
  }

  object Generator {

    def splat = new Generator {
      def apply(url: Url) = url addSplat
    }

    def literal(text: String) = new Generator {
      def apply(url: Url) = url add text
    }

    def prefixedOptional(prefix: String, name: String) = new Generator {
      def apply(url: Url) = url.params get name match {
        case Some(value) => url add (prefix + value)
        case None => url
      }
    }

    def optional(name: String) = new Generator {
      def apply(url: Url) = url add (url.params get name getOrElse "")
    }

    def named(name: String) = new Generator {
      def apply(url: Url) = url.params get name match {
        case Some(value) => url add value
        case None => throw new Exception("Url \"%s\" requires param \"%s\"" format (pattern, name))
      }
    }
  }

  object GeneratorParser extends RegexParsers {

    def apply(pattern: String): Generator = {
      parseAll(tokens, pattern) match {
        case Success(g, _) => g
        case _ => throw new Exception("Url generation fail: " + pattern)
      }
    }

    private def tokens: Parser[Generator] = rep(token) ^^ (_ reduceLeft { _ + _ })

    private def token: Parser[Generator] = splat | prefixedOptional | optional | named | literal

    private def splat: Parser[Generator] = "*" ^^^ { Generator.splat }

    private def prefixedOptional: Parser[Generator] =
      ("." | "/") ~ "?:" ~ """\w+""".r ~ "?" ^^ {
        case p ~ "?:" ~ o ~ "?" => Generator.prefixedOptional(p, o)
      }

    private def optional: Parser[Generator] =
      "?:" ~> """\w+""".r <~ "?" ^^ { Generator.optional(_) }

    private def named: Parser[Generator] =
      ":" ~> """\w+""".r ^^ { Generator.named(_) }

    private def literal: Parser[Generator] =
      ("""[\.\+\(\)\$]""".r | ".".r) ^^ { Generator.literal(_) }
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
