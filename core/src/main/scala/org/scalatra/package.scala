package org

package object scalatra 
  extends Control // make halt and pass visible to helpers outside the DSL
{
  import util.MultiMap

  type RouteTransformer = (Route => Route)

  @deprecated("Use CsrfTokenSupport", "2.0.0")
  type CSRFTokenSupport = CsrfTokenSupport
  
  type MultiParams = MultiMap

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment".intern

  val MultiParamsKey = "org.scalatra.MultiParams".intern
  
  @deprecated("Use org.scalatra.servlet.ServletBase if you depend on the Servlet API, or org.scalatra.ScalatraBase if you don't.", "2.1.0")
  type ScalatraKernel = servlet.ServletBase
}
