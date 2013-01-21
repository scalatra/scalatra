package org

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

package object scalatra
  extends Control // make halt and pass visible to helpers outside the DSL
  //  with DefaultValues // make defaults visible
{
  import util.MultiMap

  type RouteTransformer = (Route => Route)

  type MultiParams = MultiMap

  type Params = util.MultiMapHeadView[String, String] with util.MapWithIndifferentAccess[String]

  type Action = (ActionContext) => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment"

  val MultiParamsKey = "org.scalatra.MultiParams"
}
