package org.scalatra
package pattern

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




