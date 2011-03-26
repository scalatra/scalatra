package org.scalatra

import collection.mutable
import java.io.{FileInputStream, File}
import util.using
import util.io.zeroCopy

// Perhaps making renderResponseBody a stackable method this would also give a render pipeline maybe even a better one at that
//trait RenderResponseBody {
//  def renderResponseBody(actionResult: Any)
//}

/**
 * Allows overriding and chaining of response body rendering. Overrides [[ScalatraKernel#renderResponseBody]].
 */
trait RenderPipeline {this: ScalatraKernel =>

  object ActionRenderer{
    def apply[A: ClassManifest](fun: A => Any) = new ActionRenderer(fun)
  }
  private[scalatra] class ActionRenderer[A: ClassManifest](fun: A => Any) extends PartialFunction[Any, Any] {
    def apply(v1: Any) = fun(v1.asInstanceOf[A])
    def isDefinedAt(x: Any) = implicitly[ClassManifest[A]].erasure.isInstance(x)
  }

  private type RenderAction = PartialFunction[Any, Any]
  protected val renderPipeline = new mutable.ArrayBuffer[RenderAction] with mutable.SynchronizedBuffer[RenderAction]

  override def renderResponseBody(actionResult: Any) {
    (useRenderPipeline orElse defaultRenderResponse) apply actionResult
  }

  private def useRenderPipeline: PartialFunction[Any, Any] = {
    case pipelined if renderPipeline.exists(_.isDefinedAt(pipelined)) => {
      (pipelined /: renderPipeline) {
        case (body, renderer) if (renderer.isDefinedAt(body)) => renderer(body)
        case (body, _) => body
      }
    }
  }

  private def defaultRenderResponse: PartialFunction[Any, Any] = {
    case bytes: Array[Byte] =>
      response.getOutputStream.write(bytes)
    case file: File =>
      using(new FileInputStream(file)) { in => zeroCopy(in, response.getOutputStream) }
    case _: Unit =>
    // If an action returns Unit, it assumes responsibility for the response
    case x: Any  =>
      response.getWriter.print(x.toString)
  }

  /**
   * Prepend a new renderer to the front of the render pipeline.
   */
  def render[A: Manifest](fun: A => Any) {
    ActionRenderer(fun) +=: renderPipeline
  }


}

trait DefaultRendererPipeline { self: ScalatraKernel with RenderPipeline =>
  render[Any] {
    case _: Unit => // If an action or renderer returns Unit, it assumes responsibility for the response
    case x => response.getWriter.print(x.toString)
  }

  render[File] {file =>
    using(new FileInputStream(file)) {in => zeroCopy(in, response.getOutputStream)}
  }

  render[Array[Byte]] {bytes =>
    response.getOutputStream.write(bytes)
  }
}