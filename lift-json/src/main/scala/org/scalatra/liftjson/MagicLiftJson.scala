package org.scalatra
package liftjson

import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.JValue

trait MagicLiftJson extends LiftJsonSupport {
  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private def renderToJson: RenderPipeline = {
    case a: JValue => super.renderPipeline(a)
    case p if format == "json" || format == "xml" => Extraction.decompose(p)
  }
}
