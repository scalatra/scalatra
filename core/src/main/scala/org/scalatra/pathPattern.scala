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

object PathPatternParser extends RegexParsers {
  /**
   * Parses a string into a PathPattern.
   *
   * @param pattern the string to parse
   * @return a path pattern
   */
  def parseFrom(pattern: String): PathPattern =
    parseAll(pathPattern, pattern) match {
      case Success(pathPattern, _) =>
        PathPattern("^".r) + pathPattern + PathPattern("$".r)
      case _ =>
        throw new IllegalArgumentException("Invalid path pattern: " + pattern)
    }

  def pathPattern = directories ~ opt("/") ^^ {
    case dirs ~ Some(slash) => dirs + PathPattern("/".r)
    case dirs ~ None => dirs
  }

  def directories = rep(directory) ^^ { _.reduceLeft { _ + _ } }
  def directory = ("/?" | "/") ~ (splatDirectory | optionalNamedGroup | namedFileAndExtension | namedGroup | simpleDirectory) ^^ {
    case separator ~ pattern => PathPattern(separator.r) + pattern
  }

  def splatDirectory = "*" ^^^ PathPattern("(.*?)".r, List("splat"))

  def optionalNamedGroup = ":" ~> optionalPathParticle <~ "?" ^^
    { groupName => PathPattern("([^/?]+)?".r, List(groupName)) }

  def namedFileAndExtension = ":" ~> filePathParticle ~ ".:" ~ pathParticle ^^ {
    case fileGroupName ~ _ ~ extensionGroupName =>
      PathPattern("""([^/?]+)\.([^/?]+)""".r, List(fileGroupName, extensionGroupName))
  }

  def namedGroup = ":" ~> pathParticle ^^
    { groupName => PathPattern("([^/?]+)".r, List(groupName)) }

  def simpleDirectory = pathParticle ^^
    { pathPartString => PathPattern(escape(pathPartString).r, Nil) }

  def filePathParticle = """[^.]+"""r
  def optionalPathParticle = """[^/?]+"""r
  def pathParticle = """[^/]+"""r
}
