package org.scalatra
package json

import org.json4s._
import java.io.File
import xml.NodeSeq
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait JValueResult extends ScalatraSyntax { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline(implicit ctx: ActionContext): RenderPipeline = renderToJson orElse super.renderPipeline

  private[this] def renderToJson(implicit ctx: ActionContext): RenderPipeline = {
    case a: JValue => super.renderPipeline(ctx)(a)
    case status: Int => super.renderPipeline(ctx)(status)
    case bytes: Array[Byte] => super.renderPipeline(ctx)(bytes)
    case is: java.io.InputStream => super.renderPipeline(ctx)(is)
    case file: File => super.renderPipeline(ctx)(file)
    case a: ActionResult => super.renderPipeline(ctx)(a)
    case _: Unit | Unit => super.renderPipeline(ctx)(())
    case s: String => super.renderPipeline(ctx)(s)
    case null if responseFormat == "json" || responseFormat == "xml" => JNull
    case null => super.renderPipeline(ctx)(null)
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