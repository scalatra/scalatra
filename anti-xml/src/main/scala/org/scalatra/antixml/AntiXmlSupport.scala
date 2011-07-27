package org.scalatra
package antixml

import com.codecommit.antixml._

/**
 * Trait which renders Anti-XML elements as serialized XML.
 */
trait AntiXmlSupport extends ScalatraKernel {

  override protected def contentTypeInfer = {
    case _: Elem => "text/html"
  }

  override protected def renderPipeline = {
    val myPipeline: PartialFunction[Any, Any] = {
      case e: Elem => XMLSerializer().serialize(e, response.getWriter)
    }
    myPipeline orElse super.renderPipeline
  }
}
