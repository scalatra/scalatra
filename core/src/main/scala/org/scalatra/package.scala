package org

package object scalatra 
  extends Control // make halt and pass visible to helpers outside the DSL
{
  import util.MultiMap

  type RouteTransformer = (Route => Route)

  @deprecated("Use CsrfTokenSupport")
  type CSRFTokenSupport = CsrfTokenSupport
  
  type MultiParams = MultiMap

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  // The servlet extensions were moved to the servlet package
  @deprecated("Use org.scalatra.servlet.Attributes") // since 2.1
  type Attributes = servlet.Attributes
  @deprecated("Use org.scalatra.servlet.AttributesMap") // since 2.1
  type AttributesMap = servlet.AttributesMap
  @deprecated("Use org.scalatra.servlet.RichRequest") // since 2.1
  type RichRequest = servlet.RichRequest
  @deprecated("Use org.scalatra.servlet.RichRequest") // since 2.1
  val RichRequest = servlet.RichRequest
  @deprecated("Use org.scalatra.servlet.RichSession") // since 2.1
  type RichSession = servlet.RichSession
  @deprecated("Use org.scalatra.servlet.RichServletContext") // since 2.1
  type RichServletContext = servlet.RichServletContext
  @deprecated("Use org.scalatra.servlet.ServletApiImplicits") // since 2.1
  type ServletApiImplicits = servlet.ServletApiImplicits
  @deprecated("Use org.scalatra.servlet") // since 2.1
  object ServletApiImplicits extends servlet.ServletApiImplicits
}
