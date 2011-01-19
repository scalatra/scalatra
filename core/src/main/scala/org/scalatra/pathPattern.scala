package org.scalatra

import scala.collection.mutable.{HashMap, ListBuffer}
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import ScalatraKernel.MultiParams

/**
 * A path pattern optionally matches a request path and extracts path
 * parameters.
 */
case class PathPattern(regex: Regex, captureGroupNames: List[String]) {
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
}

object PathPatternParser extends RegexParsers {
  /**
   * Parses a string into a PathPattern.
   *
   * @param pattern the string to parse
   * @return a path pattern
   */
  def parseFrom(pattern: String): PathPattern = {
    def tokens(pattern: String) : Iterable[PathToken] = {
      parseAll(pathPattern, pattern) match {
        case Success(tokens, _) => tokens
        case _ =>  throw new IllegalArgumentException("Invalid path pattern: " + pattern)
      }
    }
    val pathTokens = tokens(pattern)
    val regex = ("^" + pathTokens.foldLeft("")((regexString, token: PathToken) => regexString + token.regexSection) + "$").r
    val captureGroupNames = pathTokens flatMap { token => token.captureGroupName }
    PathPattern(regex, captureGroupNames.toList)
  }

  def pathPattern = directories ~ opt("/")  ^^ { case tokens ~ optionalSlash => optionalSlash match {
    case Some(_) => tokens :+ new DirectorySeparatorToken
    case None => tokens
  }}

  def directories = rep(directory)
  def directory = "/" ~> (splatDirectory | optionalNamedGroup | namedFileAndExtension | namedGroup | simpleDirectory)

  def splatDirectory = "*" ^^^ { new SplatToken }

  def optionalNamedGroup = "?:" ~> optionalPathParticle <~ "?" ^^
          { groupName => new OptionalNamedGroupToken(groupName) }

  def namedFileAndExtension = ":" ~> filePathParticle ~ ".:" ~ pathParticle ^^ {
    case fileGroupName ~ _ ~ extensionGroupName => new FileAndExtensionGroupToken(fileGroupName, extensionGroupName)
  }

  def namedGroup = ":" ~> pathParticle ^^
          { groupName => new NamedGroupToken(groupName) }

  def simpleDirectory = pathParticle ^^
          { pathPartString => new SimpleDirectoryToken(pathPartString)}

  def filePathParticle = """[^.]+"""r
  def optionalPathParticle = """[^/?]+"""r
  def pathParticle = """[^/]+"""r

  trait PathToken {
    def regexSection : String
    def captureGroupName : Iterable[String]
  }
  class SimpleDirectoryToken(pathParticle: String) extends PathToken {
    def escape(regex: String) = regex.replaceAll("([.+()$])", """\\$1""")
    def regexSection = "/" + escape(pathParticle)
    def captureGroupName = None
  }
  class DirectorySeparatorToken extends PathToken {
    def regexSection = "/"
    def captureGroupName = None
  }
  class SplatToken extends PathToken {
    def regexSection = "/(.*?)"
    def captureGroupName = Some("splat")
  }
  class NamedGroupToken(groupName: String) extends PathToken {
    def regexSection = "/([^/?]+)"
    def captureGroupName = Some(groupName)
  }
  class OptionalNamedGroupToken(groupName: String) extends PathToken {
    def regexSection = "/?([^/?]+)?"
    def captureGroupName = Some(groupName)
  }
  class FileAndExtensionGroupToken(fileGroupName: String, extensionGroupName: String) extends PathToken {
    def regexSection = """/([^/?]+)\.([^/?]+)"""
    def captureGroupName = List(fileGroupName, extensionGroupName)
  }
}
