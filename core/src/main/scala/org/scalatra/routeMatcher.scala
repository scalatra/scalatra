package org.scalatra

import org.scalatra.util.MultiMap
import org.scalatra.util.RicherString._

import scala.util.matching.Regex
import scala.util.parsing.combinator._

/**
 * A route matcher is evaluated in the context it was created and returns a
 * a (possibly empty) multi-map of parameters if the route is deemed to match.
 */
trait RouteMatcher extends RouteTransformer {
  def apply(requestPath: String): Option[MultiParams]

  def apply(route: Route): Route = Route.appendMatcher(this)(route)
}

/**
 * A route matcher from which a URI can be generated from route parameters.
 */
trait ReversibleRouteMatcher {
  /**
   * Generates a URI from a route matcher.
   *
   * @param params a map of named params extractable by the route
   * @param splats a list of splats extractable by the route
   * @return a String that would match the route with the given params and splats
   */
  def reverse(params: Map[String, String], splats: List[String]): String
}

/**
 * An implementation of Sinatra's path pattern syntax.
 */
final class SinatraRouteMatcher(pattern: String)
    extends RouteMatcher with ReversibleRouteMatcher {

  lazy val generator: (Builder => Builder) = BuilderGeneratorParser(pattern)

  def apply(requestPath: String): Option[MultiParams] = SinatraPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    generator(Builder("", params, splats)).get

  case class Builder(path: String, params: Map[String, String], splats: List[String]) {

    def addLiteral(text: String): Builder = copy(path = path + text)

    def addSplat: Builder = copy(path = path + splats.head, splats = splats.tail)

    def addNamed(name: String): Builder =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else throw new Exception("Builder \"%s\" requires param \"%s\"" format (pattern, name))

    def addOptional(name: String): Builder =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else this

    def addPrefixedOptional(name: String, prefix: String): Builder =
      if (params contains name) copy(path = path + prefix + params(name), params = params - name)
      else this

    // checks all splats are used, appends additional params as a query string
    def get: String = {
      if (!splats.isEmpty) throw new Exception("Too many splats for builder \"%s\"" format pattern)
      val pairs = params map { case (key, value) => key.urlEncode + "=" + value.urlEncode }
      val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
      path + queryString
    }
  }

  object BuilderGeneratorParser extends RegexParsers {

    def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern) get

    private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
      tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
    }

    private def token: Parser[Builder => Builder] = splat | prefixedOptional | optional | named | literal

    private def splat: Parser[Builder => Builder] = "*" ^^^ { builder => builder addSplat }

    private def prefixedOptional: Parser[Builder => Builder] =
      ("." | "/") ~ "?:" ~ """\w+""".r ~ "?" ^^ {
        case p ~ "?:" ~ o ~ "?" => builder => builder addPrefixedOptional (o, p)
      }

    private def optional: Parser[Builder => Builder] =
      "?:" ~> """\w+""".r <~ "?" ^^ { str => builder => builder addOptional str }

    private def named: Parser[Builder => Builder] =
      ":" ~> """\w+""".r ^^ { str => builder => builder addNamed str }

    private def literal: Parser[Builder => Builder] =
      ("""[\.\+\(\)\$]""".r | ".".r) ^^ { str => builder => builder addLiteral str }
  }

  override def toString = pattern
}

/**
 * An implementation of Rails' path pattern syntax
 */
final class RailsRouteMatcher(pattern: String)
    extends RouteMatcher with ReversibleRouteMatcher {

  lazy val generator: (Builder => Builder) = BuilderGeneratorParser(pattern)

  def apply(requestPath: String): Option[MultiParams] = RailsPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    generator(Builder("", params)).get

  override def toString: String = pattern

  case class Builder(path: String, params: Map[String, String]) {

    def addStatic(text: String): Builder = copy(path = path + text)

    def addParam(name: String): Builder =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else throw new Exception("Builder \"%s\" requires param \"%s\"" format (pattern, name))

    def optional(builder: Builder => Builder): Builder =
      try builder(this)
      catch { case e: Exception => this }

    // appends additional params as a query string
    def get: String = {
      val pairs = params map { case (key, value) => key.urlEncode + "=" + value.urlEncode }
      val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
      path + queryString
    }

  }

  object BuilderGeneratorParser extends RegexParsers {

    def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern) get

    private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
      tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
    }

    //private def token = param | glob | optional | static
    private def token: Parser[Builder => Builder] = param | glob | optional | static

    private def param: Parser[Builder => Builder] =
      ":" ~> identifier ^^ { str => builder => builder addParam str }

    private def glob: Parser[Builder => Builder] =
      "*" ~> identifier ^^ { str => builder => builder addParam str }

    private def optional: Parser[Builder => Builder] =
      "(" ~> tokens <~ ")" ^^ { subBuilder => builder => builder optional subBuilder }

    private def static: Parser[Builder => Builder] =
      (escaped | char) ^^ { str => builder => builder addStatic str }

    private def identifier: Regex = """[a-zA-Z_]\w*""".r

    private def escaped: Parser[String] = literal("\\") ~> (char | paren)

    private def char: Parser[String] = metachar | stdchar

    private def metachar: Regex = """[.^$|?+*{}\\\[\]-]""".r

    private def stdchar: Regex = """[^()]""".r

    private def paren: Parser[String] = ("(" | ")")

  }
}

final class PathPatternRouteMatcher(pattern: PathPattern)
    extends RouteMatcher {

  def apply(requestPath: String): Option[MultiParams] = pattern(requestPath)

  override def toString: String = pattern.regex.toString
}

/**
 * A route matcher for regular expressions.  Useful for cases that are
 * more complex than are supported by Sinatra- or Rails-style routes.
 */
final class RegexRouteMatcher(regex: Regex)
    extends RouteMatcher {

  /**
   * Evaluates the request path against the regular expression.
   *
   * @return If the regex matches the request path, returns a list of all
   * captured groups in a "captures" variable.  Otherwise, returns None.
   */
  def apply(requestPath: String): Option[MultiMap] = regex.findFirstMatchIn(requestPath) map {
    _.subgroups match {
      case Nil => MultiMap()
      case xs => Map("captures" -> xs)
    }
  }

  override def toString: String = regex.toString
}

/**
 * A route matcher on a boolean condition.  Does not extract any route parameters.
 */
final class BooleanBlockRouteMatcher(block: => Boolean) extends RouteMatcher {

  /**
   * Evaluates the block.
   *
   * @return Some empty map if the block evaluates to true, or else None.
   */
  def apply(requestPath: String): Option[MultiMap] = if (block) Some(MultiMap()) else None

  override def toString: String = "[Boolean Guard]"
}

final class StatusCodeRouteMatcher(codes: Range, responseStatus: => Int) extends RouteMatcher {

  def apply(requestPath: String): Option[MultiMap] = if (codes.contains(responseStatus)) Some(MultiMap()) else None

  override def toString: String = codes.toString()
}
