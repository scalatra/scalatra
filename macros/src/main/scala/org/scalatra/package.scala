package org

import org.scalatra.util.{MapWithIndifferentAccess, MultiMapHeadView}

package object scalatra
  extends Control
{
  type RouteTransformer = (Route => Route)

  type MultiParams = util.MultiMap

  type Params = MultiMapHeadView[String, String] with MapWithIndifferentAccess[String]

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment"

  val MultiParamsKey = "org.scalatra.MultiParams"
}
