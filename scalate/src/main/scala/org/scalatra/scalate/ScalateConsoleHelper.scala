//package org.scalatra
//package scalate
//
//import _root_.java.util.regex.Pattern
//import _root_.org.fusesource.scalate.{DefaultRenderContext, RenderContext}
//import _root_.scala.Option
//import java.io.File
//import scala.io.Source
//import collection.JavaConversions._
//import collection.immutable.SortedMap
//import collection.mutable.{ArrayBuffer, ListBuffer}
//import scala.util.parsing.input.{Position, OffsetPosition}
//import xml.NodeSeq
//import org.fusesource.scalate.util.{Log, SourceMapInstaller, SourceMap}
//import org.fusesource.scalate.console.ConsoleSnippets
//
//case class SourceLine(line: Int, source: String) {
//  def style(errorLine: Int): String = if (line == errorLine) "line error" else "line"
//
//  def nonBlank = source != null && source.length > 0
//
//
//  /**
//   * Return a tuple of the prefix, the error character and the postfix of this source line
//   * to highlight the error at the given column
//   */
//  def splitOnCharacter(col: Int): Tuple3[String, String, String] = {
//    val length = source.length
//    if (col >= length) {
//      (source, "", "")
//    }
//    else {
//      val next = col + 1
//      val prefix = source.substring(0, col)
//      val ch = if (col < length) source.substring(col, next) else ""
//      val postfix = if (next < length) source.substring(next, length) else ""
//      (prefix, ch, postfix)
//    }
//  }
//}
//
//object ScalateConsoleHelper extends Log
//class ScalateConsoleHelper(context: RenderContext) extends ConsoleSnippets {
//  import ScalateConsoleHelper._
//  import context._
//
//
//}
