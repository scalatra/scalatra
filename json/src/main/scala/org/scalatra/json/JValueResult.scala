package org.scalatra
package json

import org.json4s._
import java.io.File
import xml.NodeSeq
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait JValueResult extends ScalatraSyntax { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline(implicit request: HttpServletRequest, response: HttpServletResponse): RenderPipeline = renderToJson orElse super.renderPipeline

  private[this] def renderToJson(implicit request: HttpServletRequest, response: HttpServletResponse): RenderPipeline = {
    case a: JValue => super.renderPipeline(request, response)(a)
    case status: Int => super.renderPipeline(request, response)(status)
    case bytes: Array[Byte] => super.renderPipeline(request, response)(bytes)
    case is: java.io.InputStream => super.renderPipeline(request, response)(is)
    case file: File => super.renderPipeline(request, response)(file)
    case a: ActionResult => super.renderPipeline(request, response)(a)
    case _: Unit | Unit => super.renderPipeline(request, response)(())
    case s: String => super.renderPipeline(request, response)(s)
    case null if responseFormat == "json" || responseFormat == "xml" => JNull
    case null => super.renderPipeline(request, response)(null)
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