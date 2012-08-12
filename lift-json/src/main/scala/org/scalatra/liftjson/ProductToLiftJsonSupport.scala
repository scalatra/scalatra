package org.scalatra
package liftjson

import net.liftweb.json.Extraction

trait ProductToLiftJsonSupport extends LiftJsonSupport {
  override protected def renderPipeline: RenderPipeline = renderProductToJson orElse super.renderPipeline

  private def renderProductToJson: RenderPipeline = {
    case p: Product if format == "json" || format == "xml" => Extraction.decompose(p)
  }
}
