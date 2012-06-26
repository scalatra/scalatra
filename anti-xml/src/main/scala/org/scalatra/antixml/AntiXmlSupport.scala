package org.scalatra
package antixml

import com.codecommit.antixml._

/**
 * Trait which renders Anti-XML elements as serialized XML.
 */
trait AntiXmlSupport extends ScalatraApp {

  override protected def contentTypeInferrer = ({
    case _: Elem => "text/html"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  override protected def renderPipeline = ({
    case e: Elem => XMLSerializer().serialize(e, response.writer)
  }: RenderPipeline) orElse super.renderPipeline
}
