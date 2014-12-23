package org

import org.scalatra.servlet.FileUploadSupport
import org.scalatra.util.{ MapWithIndifferentAccess, MultiMapHeadView }

package object scalatra
    extends Control // make halt and pass visible to helpers outside the DSL
    //  with DefaultValues // make defaults visible
    {
  import org.scalatra.util.MultiMap

  object RouteTransformer {
    implicit def fn2transformer(fn: Route => Route) = new RouteTransformer {
      override def apply(route: Route): Route = fn(route)
    }
  }
  trait RouteTransformer {
    def apply(route: Route): Route
  }

  @deprecated("Use ContentEncodingSupport, GZipSupport will be removed eventually", "2.4")
  type GZipSupport = ContentEncodingSupport

  type MultiParams = MultiMap

  type Params = MultiMapHeadView[String, String] with MapWithIndifferentAccess[String]

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment"

  val MultiParamsKey = "org.scalatra.MultiParams"

  @deprecated("Use org.scalatra.servlet.ServletBase if you depend on the Servlet API, or org.scalatra.ScalatraBase if you don't.", "2.1.0")
  type ScalatraKernel = servlet.ServletBase

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

  //  class OptionDefaults[T](value: Option[T]) {
  //	  /**
  //	   * Returns the item contained in the Option if it is defined, otherwise, the default element for the type A
  //	   * <p/>
  //	   * For example:
  //	   * <pre>
  //	   * val o: Option = None
  //	   * val a: List[String] = ~o
  //	   * </pre>
  //	   */
  //	  def unary_~(implicit z: DefaultValue[T]): T = value getOrElse z.default
  //	
  //	  def orDefault(implicit z: DefaultValue[T]): T = value getOrElse z.default
  //	}
  //  implicit def option2optionWithDefault[T](opt: Option[T]) = new OptionDefaults(opt)
}
