package org.scalatra
package json

import org.json4s._
import util._
import io._
import org.scalatra.ActionResult
import java.io.{FileInputStream, File}
import org.scalatra.ActionResult

trait JValueResult extends ScalatraBase { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private def renderToJson: RenderPipeline = {
    case a: JValue => super.renderPipeline(a)
    case status: Int => super.renderPipeline(status)
    case bytes: Array[Byte] => super.renderPipeline(bytes)
    case is: java.io.InputStream => super.renderPipeline(is)
    case file: File => super.renderPipeline
    case a: ActionResult => super.renderPipeline(a)
    case _: Unit | Unit => super.renderPipeline(())
    case s: String => super.renderPipeline(s)
    case p: Product if responseFormat == "json" || responseFormat == "xml" => Extraction.decompose(p)
    case p: Traversable[_] if responseFormat == "json" || responseFormat == "xml" => Extraction.decompose(p)
    case a => super.renderPipeline(a)
  }

}