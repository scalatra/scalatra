package org.scalatra
package jackson

import io.Codec
import xml.XML

trait ProductToJacksonSupport extends JacksonSupport {
  import org.scalatra.json.JsonOutput._
  override protected def renderPipeline: RenderPipeline = renderProduct orElse super.renderPipeline

  private[this] def renderProduct: RenderPipeline = {
    case p: Product if format == "json" =>
      // JSON is always UTF-8
      response.characterEncoding = Some(Codec.UTF8.name)
      val writer = response.writer

      val jsonpCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield callback

      jsonpCallback match {
        case some :: _ =>
          // JSONP is not JSON, but JavaScript.
          contentType = formats("js")
          // Status must always be 200 on JSONP, since it's loaded in a <script> tag.
          status = 200
          writer write some
          writer write '('
          writer write jsonMapper.writeValueAsString(p)
          writer write ");"
        case _ =>
          if(jsonVulnerabilityGuard) writer.write(VulnerabilityPrelude)
          writer write jsonMapper.writeValueAsString(p)
        ()
      }

    case p: Product if format == "xml" =>
      val nodes = xmlRootNode.copy(child = XML.loadString(xmlMapper.writeValueAsString(p)).child)
      XML.write(response.writer, xml.Utility.trim(nodes), response.characterEncoding.get, xmlDecl = true, null)

  }
}
