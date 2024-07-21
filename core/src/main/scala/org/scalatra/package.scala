package org

import org.scalatra.servlet.FileUploadSupport
import org.scalatra.util.MultiMapHeadView
import java.lang.{ Integer => JInteger }

package object scalatra {

  type MultiParams = Map[String, Seq[String]]

  type Params = MultiMapHeadView[String, String]

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

  /**
   * Immediately halts processing of a request.  Can be called from either a
   * before filter or a route.
   *
   * @param status the status to set on the response, or null to leave
   *        the status unchanged.
   * @param body a result to render through the render pipeline as the body
   * @param headers headers to add to the response
   */
  def halt[T](
    status: JInteger = null,
    body: T = (),
    headers: Map[String, String] = Map.empty): Nothing = {
    val statusOpt = if (status == null) None else Some(status.intValue)
    throw new HaltException(statusOpt, headers, body)
  }

  def halt(result: ActionResult): Nothing = {
    halt(result.status, result.body, result.headers)
  }

  /**
   * Immediately exits from the current route.
   */
  def pass(): Nothing = throw new PassException
}
