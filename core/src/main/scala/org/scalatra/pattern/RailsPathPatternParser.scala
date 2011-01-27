package org.scalatra
package pattern

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