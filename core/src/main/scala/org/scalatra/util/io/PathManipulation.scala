package org.scalatra
package util
package io

object PathManipulationOps {

  def ensureSlash(candidate: String) = {
    (candidate.startsWith("/"), candidate.endsWith("/")) match {
      case (true, true) => candidate.dropRight(1)
      case (true, false) => candidate
      case (false, true) => "/" + candidate.dropRight(1)
      case (false, false) => "/" + candidate
    }
  }

}

trait PathManipulation  {

  import PathManipulationOps._
  def basePath: String
  def pathName: String
  lazy val appPath: String = absolutizePath(basePath) / pathName

  def normalizePath(pth: String) = (ensureSlash _ compose absolutizePath _)(pth)
  def splitPaths(path: String) = {
    val norm = normalizePath(path)
    val parts = norm split "/"
    (absolutizePath(parts dropRight 1 mkString "/"), parts.lastOption getOrElse "")
  }


  protected def absolutizePath(path: String): String = {
    path.blankOption map (p => ensureSlash(if (p.startsWith("/")) p else appPath / p)) getOrElse appPath
  }
}
