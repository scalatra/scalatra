package org.scalatra
package json

import org.json4s._

trait JValueResult extends ScalatraBase { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private def renderToJson: RenderPipeline = {
    case a: JValue => super.renderPipeline(a)
    case _: Unit | Unit => super.renderPipeline(())
    case p if responseFormat == "json" || responseFormat == "xml" => Extraction.decompose(p)
  }

}