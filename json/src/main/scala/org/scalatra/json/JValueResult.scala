package org.scalatra
package json

import org.json4s._
import util._
import io._
import org.scalatra.ActionResult
import java.io.{FileInputStream, File}
import org.scalatra.ActionResult
import xml.{Node, NodeSeq}

trait JValueResult extends ScalatraBase { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private[this] def renderToJson: RenderPipeline = {
    case a: JValue => super.renderPipeline(a)
    case status: Int => super.renderPipeline(status)
    case bytes: Array[Byte] => super.renderPipeline(bytes)
    case is: java.io.InputStream => super.renderPipeline(is)
    case file: File => super.renderPipeline(file)
    case a: ActionResult => super.renderPipeline(a)
    case _: Unit | Unit => super.renderPipeline(())
    case s: String => super.renderPipeline(s)
    case null if responseFormat == "json" || responseFormat == "xml" => JNull
    case null => super.renderPipeline(null)
    case x: scala.xml.Node if responseFormat == "xml" ⇒
      contentType = formats("xml")
      response.writer.write(scala.xml.Utility.trim(x).toString())
    case x: NodeSeq if responseFormat == "xml" ⇒
      contentType = formats("xml")
      response.writer.write(x.toString)
    case x: NodeSeq ⇒
      response.writer.write(x.toString)
    case p: Product if responseFormat == "json" || responseFormat == "xml" => Extraction.decompose(p)
    case p: Traversable[_] if responseFormat == "json" || responseFormat == "xml" => Extraction.decompose(p)
  }

}