package org.scalatra
package servlet

import scala.language.experimental.macros

import javax.servlet.AsyncEvent
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

object AsyncSupport {
  val ExecutionContextKey = "org.scalatra.ExecutionContext"
}

trait AsyncSupport extends ServletBase with ScalatraAsyncSupport {

  /**
   * Takes a block and converts it to an action that can be run asynchronously.
   */
  protected def asynchronously(f: => Any): Action

  protected def onAsyncEvent(event: AsyncEvent)(thunk: => Any): Unit = {
    withRequest(event.getSuppliedRequest.asInstanceOf[HttpServletRequest]) {
      withResponse(event.getSuppliedResponse.asInstanceOf[HttpServletResponse]) {
        thunk
      }
    }
  }

  protected def withinAsyncContext(context: javax.servlet.AsyncContext)(thunk: => Any): Unit = {
    withRequest(context.getRequest.asInstanceOf[HttpServletRequest]) {
      withResponse(context.getResponse.asInstanceOf[HttpServletResponse]) {
        thunk
      }
    }
  }

  /**
   * The Scalatra DSL core methods take a list of [[org.scalatra.RouteMatcher]]
   * and a block as the action body.  The return value of the block is
   * rendered through the pipeline and sent to the client as the response body.
   * The block of these methods is executed in a lightweight event-driven thread
   * from Akka's dispatchers
   *
   * See [[org.scalatra.ScalatraBaseBase#renderResponseBody]] for the detailed
   * behaviour and how to handle your response body more explicitly, and see
   * how different return types are handled.
   *
   * The block is executed in the context of a CoreDsl instance, so all the
   * methods defined in this trait are also available inside the block.
   *
   * {{{
   *   asyncGet("/") {
   *     <form action="/echo">
   *       <label>Enter your name</label>
   *       <input type="text" name="name"/>
   *     </form>
   *   }
   *
   *   asyncPost("/echo") {
   *     s"hello {params('name)}!"
   *   }
   * }}}
   *
   * ScalatraKernel provides implicit transformation from boolean blocks,
   * strings and regular expressions to [[org.scalatra.RouteMatcher]], so
   * you can write code naturally.
   * {{{
   *   asyncGet("/", request.getRemoteHost == "127.0.0.1") { "Hello localhost!" }
   * }}}
   *
   */
  def asyncGet(transformers: RouteTransformer*)(block: => Any): Route = macro CoreDslMacros.asyncGetImpl

  /**
   * @see asyncGet
   */
  def asyncPost(transformers: RouteTransformer*)(block: => Any): Route = macro CoreDslMacros.asyncPostImpl

  /**
   * @see asyncGet
   */
  def asyncPut(transformers: RouteTransformer*)(block: => Any): Route = macro CoreDslMacros.asyncPutImpl

  /**
   * @see asyncGet
   */
  def asyncDelete(transformers: RouteTransformer*)(block: => Any): Route = macro CoreDslMacros.asyncDeleteImpl

  /**
   * @see asyncGet
   */
  def asyncOptions(transformers: RouteTransformer*)(block: => Any): Route = macro CoreDslMacros.asyncOptionsImpl

  /**
   * @see asyncGet
   */
  def asyncPatch(transformers: RouteTransformer*)(block: => Any): Route = macro CoreDslMacros.asyncPatchImpl

}
