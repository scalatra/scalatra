package org

import org.scalatra.servlet.FileUploadSupport
import org.scalatra.util.{ MultiMap, MapWithIndifferentAccess, MultiMapHeadView }

package object scalatra
  extends Control // make halt and pass visible to helpers outside the DSL
  //  with DefaultValues // make defaults visible
  {

  object RouteTransformer {

    implicit def fn2transformer(fn: Route => Route): RouteTransformer = new RouteTransformer {
      override def apply(route: Route): Route = fn(route)
    }
  }

  trait RouteTransformer {
    def apply(route: Route): Route
  }

  type MultiParams = Map[String, Seq[String]]

  type Params = MultiMapHeadView[String, String] with MapWithIndifferentAccess[String]

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment"

  val MultiParamsKey = "org.scalatra.MultiParams"

  type CoreStackNoFlash = CorsSupport with FutureSupport
  type CoreStackNoFlashWithCsrf = CoreStackNoFlash with CsrfTokenSupport
  type CoreStackNoFlashWithXsrf = CoreStackNoFlash with XsrfTokenSupport

  type FuturesAndFlashStack = FutureSupport with FlashMapSupport
  type FuturesAndFlashStackWithCsrf = FuturesAndFlashStack with CsrfTokenSupport
  type FuturesAndFlashStackWithXsrf = FuturesAndFlashStack with XsrfTokenSupport

  type CoreStack = CorsSupport with FutureSupport with FlashMapSupport
  type CoreStackWithCsrf = CoreStack with CsrfTokenSupport
  type CoreStackWithXsrf = CoreStack with XsrfTokenSupport

  type FullCoreStack = CoreStack with FileUploadSupport
  type FileUploadStack = FutureSupport with FlashMapSupport with FileUploadSupport

}
