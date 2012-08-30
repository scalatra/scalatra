package org.scalatra
package json

import org.json4s._

trait JValueResult extends ScalatraBase { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private def renderToJson: RenderPipeline = {
    case a: JValue => super.renderPipeline(a)
    case p if format == "json" || format == "xml" => Extraction.decompose(p)
  }
}