package org.scalatra
package json

import org.json4s._
import util._
import io._
import org.scalatra.ActionResult
import java.io.{FileInputStream, File}
import org.scalatra.ActionResult
import xml.{Node, NodeSeq}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait JValueResult extends ScalatraBase { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  //override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private[this] def isJValueResponse(implicit request: HttpServletRequest, response: HttpServletResponse) =
          responseFormat == "json" || responseFormat == "xml"

  private[this] def customSerializer = jsonFormats.customSerializer

  // of type RenderPipeline
  override protected def renderPipeline(req: HttpServletRequest, resp: HttpServletResponse, body: Any): Any = {
    implicit val rq = req
    implicit val rs = resp
    body match {
      case JNull | JNothing =>
      case a: JValue => super.renderPipeline(req, resp, a)
      case a: Any if isJValueResponse && customSerializer.isDefinedAt(a) =>
        customSerializer.lift(a) match {
          case Some(jv: JValue) => jv
          case None => super.renderPipeline(req, resp, a)
        }
      case status: Int => super.renderPipeline(req, resp, status)
      case bytes: Array[Byte] => super.renderPipeline(req, resp, bytes)
      case is: java.io.InputStream => super.renderPipeline(req, resp, is)
      case file: File => super.renderPipeline(req, resp, file)
      case a: ActionResult => super.renderPipeline(req, resp, a)
      case _: Unit | Unit | null =>
      case s: String => super.renderPipeline(req, resp, s)
      case x: scala.xml.Node if responseFormat == "xml" ⇒
        contentType = formats("xml")
        resp.writer.write(scala.xml.Utility.trim(x).toString())
      case x: NodeSeq if responseFormat == "xml" ⇒
        contentType = formats("xml")
        resp.writer.write(x.toString)
      case x: NodeSeq ⇒
        resp.writer.write(x.toString)
      case p: Product if isJValueResponse => Extraction.decompose(p)
      case p: TraversableOnce[_] if isJValueResponse => Extraction.decompose(p)

      case _ => super.renderPipeline(req, resp, body)
    }
  }

}