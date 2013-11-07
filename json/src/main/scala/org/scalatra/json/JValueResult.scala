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

  private[this] def isJValueResponse = responseFormat == "json" || responseFormat == "xml"

  private[this] def customSerializer = jsonFormats.customSerializer

  private[this] def renderToJson: RenderPipeline = {
    case JNull | JNothing =>
    case a: JValue => super.renderPipeline(a)
    case a: Any if isJValueResponse && customSerializer.isDefinedAt(a) =>
      customSerializer.lift(a) match {
        case Some(jv: JValue) => jv
        case None => super.renderPipeline(a)
      }
    case status: Int => super.renderPipeline(status)
    case bytes: Array[Byte] => super.renderPipeline(bytes)
    case is: java.io.InputStream => super.renderPipeline(is)
    case file: File => super.renderPipeline(file)
    case a: ActionResult => super.renderPipeline(a)
    case _: Unit | Unit | null =>
    case s: String => super.renderPipeline(s)
    case x: scala.xml.Node if responseFormat == "xml" ⇒
      contentType = formats("xml")
      response.writer.write(scala.xml.Utility.trim(x).toString())
    case x: NodeSeq if responseFormat == "xml" ⇒
      contentType = formats("xml")
      response.writer.write(x.toString)
    case x: NodeSeq ⇒
      response.writer.write(x.toString)
    case p: Product if isJValueResponse => Extraction.decompose(p)
    case p: TraversableOnce[_] if isJValueResponse => Extraction.decompose(p)
  }

}