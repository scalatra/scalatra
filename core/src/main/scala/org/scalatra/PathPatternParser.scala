package org.scalatra

import scala.util.parsing.combinator.RegexParsers

object PathPatternParser extends RegexParsers {
  def parseFrom(path: String) = {
    def tokens(path: String) : Iterable[PathToken] = {
      parseAll(pathPattern, path) match {
        case Success(tokens, _) => tokens
        case _ =>  throw new IllegalArgumentException("Invalid path pattern: " + path)
      }
    }
    val pathTokens = tokens(path)
    val regex = ("^" + pathTokens.foldLeft("")((regexString, token: PathToken) => regexString + token.regexSection) + "$").r
    val captureGroupNames = pathTokens flatMap { token => token.captureGroupName } 
    (regex, captureGroupNames)
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