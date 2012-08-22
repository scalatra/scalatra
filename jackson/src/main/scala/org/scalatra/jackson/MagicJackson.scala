package org.scalatra
package jackson

import com.fasterxml.jackson.databind.JsonNode

trait MagicJackson extends JacksonSupport {
  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  private[this] def isApplicable = format == "json" || format == "xml"

  private def renderToJson: RenderPipeline = {
    case a: JsonNode => super.renderPipeline(a)
    case p if isApplicable => jsonMapper.valueToTree[JsonNode](p)
  }
}
