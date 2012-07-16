package org.scalatra
package scalate

import _root_.java.util.regex.Pattern
import _root_.org.fusesource.scalate.{DefaultRenderContext, RenderContext}
import _root_.scala.Option
import java.io.File
import scala.io.Source
import collection.JavaConversions._
import collection.immutable.SortedMap
import collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.parsing.input.{Position, OffsetPosition}
import xml.NodeSeq
import org.fusesource.scalate.util.{Log, SourceMapInstaller, SourceMap}
import org.fusesource.scalate.console.{Archetype, ConsoleSnippets}

case class SourceLine(line: Int, source: String) {
  def style(errorLine: Int): String = if (line == errorLine) "line error" else "line"

  def nonBlank = source != null && source.length > 0


  /**
   * Return a tuple of the prefix, the error character and the postfix of this source line
   * to highlight the error at the given column
   */
  def splitOnCharacter(col: Int): Tuple3[String, String, String] = {
    val length = source.length
    if (col >= length) {
      (source, "", "")
    }
    else {
      val next = col + 1
      val prefix = source.substring(0, col)
      val ch = if (col < length) source.substring(col, next) else ""
      val postfix = if (next < length) source.substring(next, length) else ""
      (prefix, ch, postfix)
    }
  }
}

object ScalateConsoleHelper extends Log
class ScalateConsoleHelper(context: RenderContext) extends ScalateConsoleSnippets {
  import ScalateConsoleHelper._
  import context._

  val consoleParameter = "_scalate"

  def appContext: AppContext = context.asInstanceOf[ScalatraRenderContext].appContext
  def renderContext = context

  // TODO figure out the viewName from the current template?
  def viewName = "index"

  /**
   * Returns the class name of the current resource
   */
  def resourceClassName: Option[String] = attributes.get("it") match {
    case Some(it: AnyRef) => Some(it.getClass.getName)
    case _ => None
  }

  def isDevelopmentMode = context.engine.isDevelopmentMode

  /**
   * Returns an attempt at finding the source file for the current resource.
   *
   * TODO use bytecode swizzling to find the accurate name from the debug info in
   * the class file!
   */
  def resourceSourceFile: Option[File] = resourceClassName match {
    case Some(name: String) =>
      val fileName = name.replace('.', '/')
      val prefixes = List("src/main/scala/", "src/main/java/")
      val postfixes = List(".scala", ".java")

      val names = for (prefix <- prefixes; postfix <- postfixes) yield new File(prefix + fileName + postfix)
      names.find(_.exists)

    case _ => None
  }

  /**
   * Returns all the available archetypes for the current view name
   */
  def archetypes: Array[Archetype] = {
    val dir = "/WEB-INF/archetypes/" + viewName
    var files: Array[File] = Array()
    val fileName = realPath(dir)
    if (fileName != null) {
      val file = new File(fileName)
      if (file.exists && file.isDirectory) {
        files = file.listFiles
      }
    }
    files.map(f => new Archetype(new File(dir, f.getName)))
  }

  /**
   * Creates the newly created template name if there can be one for the current resource
   */
  def newTemplateName(): Option[String] = resourceClassName match {
    case Some(resource) =>
      val prefix = "/" + resource.replace('.', '/') + "."

      if (templates.exists(_.startsWith(prefix)) == false) {
        Some(prefix + viewName)
      }
      else {
        None
      }
    case _ => None
  }

  /**
   * Returns the current template names used in the current context
   */
  def templates: List[String] = attributes.get("scalateTemplates") match {
    case Some(list: List[String]) => list.distinct.sortWith(_ < _)
    case _ => Nil
  }

  /**
   * Returns the current layouts used in the current context
   */
  def layouts: List[String] = attributes.get("scalateLayouts") match {
    case Some(list: List[String]) => list.distinct.sortWith(_ < _)
      case _ => Nil
    }


    /**
     * Returns true if the option is enabled
     */
    def optionEnabled(name: String): Boolean =
      context.asInstanceOf[ScalatraRenderContext].multiParams(consoleParameter).contains(name)

    /**
     * Link to the current page with the option enabled
     */
    def enableLink(name: String): String =
      context.asInstanceOf[ScalatraRenderContext].currentUriPlus(consoleParameter + "=" + name)

    /**
     * Link to the current page with the option disabled
     */
    def disableLink(name: String): String =
      context.asInstanceOf[ScalatraRenderContext].currentUriMinus(consoleParameter + "=" + name)


    /**
     * Retrieves a chunk of lines either side of the given error line
     */
    def lines(template: String, errorLine: Int, chunk: Int): Seq[SourceLine] = {
      val file = realPath(template)
      if (file != null) {
        val source = Source.fromFile(file)
        val start = (errorLine - chunk).min(0)
        val end = start + chunk

        val list = new ListBuffer[SourceLine]
        val lines = source.getLines().toIndexedSeq
        for (i <- 1.to(end)) {
          val code = lines(i)
          if (i >= start) {
            list += SourceLine(i, code)
          }
        }
        list
      }
      else {
        Nil
      }
    }

    /**
     * Retrieves a chunk of lines either side of the given error line
     */
    def lines(template: String, pos: Position, chunk: Int = 5): Seq[SourceLine] = {
      pos match {
        case op: OffsetPosition =>

          // OffsetPosition's already are holding onto the file contents
          val index: Array[String] = {
            val source = op.source
            var rc = new ArrayBuffer[String]
            var start = 0;
            for (i <- 0 until source.length) {
              if (source.charAt(i) == '\n') {
                rc += source.subSequence(start, i).toString.stripLineEnd
                start = i + 1
              }
            }
            rc.toArray
          }


          val start = (pos.line - chunk).max(1)
          val end = (pos.line + chunk).min(index.length)

          val list = new ListBuffer[SourceLine]
          for (i <- start to end) {
            list += SourceLine(i, index(i - 1))
          }
          list


        case _ =>

          // We need to manually load the file..
          lines(template, pos.line, chunk)
      }

    }

    def systemProperties: SortedMap[String,String] = {
      // TODO is there a better way?
      val m: Map[String,String] = System.getProperties.toMap
      SortedMap(m.iterator.toSeq :_*)
    }

    // Error Handling helper methods
    //-------------------------------------------------------------------------
    def exception = attributes("javax.servlet.error.exception")

    def errorMessage = attributeOrElse("javax.servlet.error.message", "")

    def errorRequestUri = attributeOrElse("javax.servlet.error.request_uri", "")

    def errorCode = attributeOrElse("javax.servlet.error.status_code", 500)

    def renderStackTraceElement(stack:StackTraceElement): NodeSeq = {
      var rc:NodeSeq = null

      // Does it look like a scalate template class??
      var className = stack.getClassName.split(Pattern.quote(".")).last
      if( className.startsWith("$_scalate_$") ) {
        // Then try to load it's smap info..
        var file = RenderContext().engine.bytecodeDirectory
        file = new File(file, stack.getClassName.replace('.', '/')+".class")
        try {
          val smap = SourceMap.parse(SourceMapInstaller.load(file))
          // And then render a link to the original template file.
          smap.mapToStratum(stack.getLineNumber) match {
            case None =>
            case Some((file, line)) =>
              rc = editLink(file, Some(line), Some(1)) {
                RenderContext() << <pre class="stacktrace">at ({file}:{line})</pre>
              }
          }
        } catch {
          // ignore errors trying to load the smap... we can fallback
          // to rendering a plain stack line.
          case e:Throwable=>
        }
      }

      if( rc==null )
        <pre class="stacktrace">at {stack.getClassName}.{stack.getMethodName}({stack.getFileName}:{stack.getLineNumber})</pre>
      else
        rc
    }
}
