package org.scalatra

import scala.collection.mutable.{HashMap, ListBuffer}
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import java.util.regex.Pattern.{quote => escape}
import ScalatraKernel.MultiParams

/**
 * A path pattern optionally matches a request path and extracts path
 * parameters.
 */
case class PathPattern(regex: Regex, captureGroupNames: List[String] = Nil) {
  def apply(path: String): Option[MultiParams] = {
    regex.findFirstMatchIn(path)
      .map { captureGroupNames zip _.subgroups }
      .map { pairs =>
      val multiParams = new HashMap[String, ListBuffer[String]]
      pairs foreach { case (k, v) =>
        if (v != null) multiParams.getOrElseUpdate(k, new ListBuffer) += v
      }
      Map() ++ multiParams
    }
  }

  def +(pathPattern: PathPattern): PathPattern = PathPattern(
    new Regex(this.regex.toString + pathPattern.regex.toString),
    this.captureGroupNames ::: pathPattern.captureGroupNames
  )
}

/**
 * Parses a string into a path pattern for routing.
 */
trait PathPatternParser {
  def apply(pattern: String): PathPattern
}

trait RegexPathPatternParser extends PathPatternParser with RegexParsers {
  /**
   * This parser gradually builds a regular expression.  Some intermediate
   * strings are not valid regexes, so we wait to compile until the end.
   */
  protected case class PartialPathPattern(regex: String, captureGroupNames: List[String] = Nil)
  {
    def toPathPattern: PathPattern = PathPattern(regex.r, captureGroupNames)

    def +(other: PartialPathPattern): PartialPathPattern = PartialPathPattern(
      this.regex + other.regex,
      this.captureGroupNames ::: other.captureGroupNames
    )
  }
}

/**
 * A Sinatra-compatible route path pattern parser.
 */
object SinatraPathPatternParser extends RegexPathPatternParser {
  def apply(pattern: String): PathPattern =
    parseAll(pathPattern, pattern) match {
      case Success(pathPattern, _) =>
        (PartialPathPattern("^") + pathPattern + PartialPathPattern("$")).toPathPattern
      case _ =>
        throw new IllegalArgumentException("Invalid path pattern: " + pattern)
    }

  private def pathPattern = rep(token) ^^ { _.reduceLeft { _+_ } }

  private def token = splat | namedGroup | literal

  private def splat = "*" ^^^ PartialPathPattern("(.*?)", List("splat"))

  private def namedGroup = ":" ~> """\w+""".r ^^
    { groupName => PartialPathPattern("([^/?#]+)", List(groupName)) }

  private def literal = metaChar | normalChar

  private def metaChar = """[\.\+\(\)\$]""".r ^^
    { c => PartialPathPattern("\\" + c) }

  private def normalChar = ".".r ^^ { c => PartialPathPattern(c) }
}

/**
 * Path pattern parser based on Rack::Mount::Strexp, which is used by Rails.
 */
object RailsPathPatternParser extends RegexPathPatternParser {
  def apply(pattern: String): PathPattern =
    parseAll(target, pattern) match {
      case Success(target, _) => target
      case _ =>
        throw new IllegalArgumentException("Invalid path pattern: " + pattern)
    }

  private def target = expr ^^
    { e => PartialPathPattern("\\A"+e.regex+"\\Z", e.captureGroupNames).toPathPattern }

  private def expr = rep1(token) ^^
    { _.reduceLeft { _+_ } }

  private def token = param | glob | optional | static

  private def param = ":" ~> identifier ^^
    { name => PartialPathPattern("([^#/.?]+)", List(name)) }

  private def identifier = """[a-zA-Z_]\w*""".r

  private def glob = "*" ~> identifier ^^
    { name => PartialPathPattern("(.+)", List(name)) }

  private def optional: Parser[PartialPathPattern] = "(" ~> expr <~ ")" ^^
    { e => PartialPathPattern("(?:"+e.regex+")?", e.captureGroupNames) }

  private def static = (escaped | char) ^^
    { str => PartialPathPattern(str) }

  private def escaped = literal("\\") ~> (char | paren)

  private def char = metachar | stdchar

  private def metachar = """[.^$|?+*{}\\\[\]-]""".r ^^ { "\\"+_ }

  private def stdchar = """[^()]""".r

  private def paren = ("(" | ")") ^^ { "\\"+_ }
}
