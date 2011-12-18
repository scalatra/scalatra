package org.scalatra

import javax.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait AsyncSupport extends ScalatraKernel {
  import ScalatraKernel.Action

  /**
   * Takes a block and converts it to an action that can be run asynchronously.
   */
  protected def asynchronously(f: => Any): Action

  protected def onAsyncEvent(event: AsyncEvent)(thunk: => Any) {
    _request.withValue(event.getSuppliedRequest.asInstanceOf[HttpServletRequest]) {
      _response.withValue(event.getSuppliedResponse.asInstanceOf[HttpServletResponse]) {
        thunk
      }
    }
  }

  protected def withinAsyncContext(context: AsyncContext)(thunk: => Any) {
    if (context.hasOriginalRequestAndResponse) {
      _request.withValue(context.getRequest.asInstanceOf[HttpServletRequest]) {
        _response.withValue(context.getResponse.asInstanceOf[HttpServletResponse]) {
          thunk
        }
      }
    } else thunk
  }


  /**
   * The Scalatra DSL core methods take a list of [[org.scalatra.RouteMatcher]]
   * and a block as the action body.  The return value of the block is
   * rendered through the pipeline and sent to the client as the response body.
   * The block of these methods is executed in a lightweight event-driven thread
   * from Akka's dispatchers
   *
   * See [[org.scalatra.ScalatraKernel.renderResponseBody]] for the detailed
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
   *     "hello {params('name)}!"
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
  def asyncGet(routeMatchers: RouteMatcher*)(block: => Any): Route = 
    get(routeMatchers: _*)(asynchronously(block))

  /**
   * @see asyncGet
   */
  def asyncPost(routeMatchers: RouteMatcher*)(block: => Any): Route = 
    post(routeMatchers: _*)(asynchronously(block))

  /**
   * @see asyncGet
   */
  def asyncPut(routeMatchers: RouteMatcher*)(block: => Any): Route = 
    put(routeMatchers: _*)(asynchronously(block))

  /**
   * @see asyncGet
   */
  def asyncDelete(routeMatchers: RouteMatcher*)(block: => Any): Route = 
    delete(routeMatchers: _*)(asynchronously(block))

  /**
   * @see asyncGet
   */
  def asyncOptions(routeMatchers: RouteMatcher*)(block: => Any): Route = 
    options(routeMatchers: _*)(asynchronously(block))

  /**
   * @see asyncGet
   */
  def asyncPatch(routeMatchers: RouteMatcher*)(block: => Any): Route = 
    patch(routeMatchers: _*)(asynchronously(block))
}
