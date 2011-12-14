package org.scalatra
package akka

import _root_.akka.actor.Actor
import _root_.akka.dispatch.Future
import _root_.akka.util.duration._
import org.scalatra.ScalatraKernel
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}



trait AkkaSupport extends ScalatraKernel {

  private def withAsyncRequestAndResponse(event: AsyncEvent)(thunk: => Any) {
    _request.withValue(event.getSuppliedRequest.asInstanceOf[HttpServletRequest]) {
      _response.withValue(event.getSuppliedResponse.asInstanceOf[HttpServletResponse]) {
        thunk
      }
    }
  }

  private def withAsyncRequestAndResponse(event: AsyncContext)(thunk: => Any) {
    if (event.hasOriginalRequestAndResponse) {
      _request.withValue(event.getRequest.asInstanceOf[HttpServletRequest]) {
        _response.withValue(event.getResponse.asInstanceOf[HttpServletResponse]) {
          thunk
        }
      }
    } else thunk
  }

  override protected def renderResponseBody(actionResult: Any) = {
    actionResult match {
      case f: Future[_] => {
        val gotResponseAlready = new AtomicBoolean(false)
        val context = request.startAsync()
        context.setTimeout(f.timeoutInNanos.nanos.toMillis)
        context addListener (new AsyncListener {
          def onComplete(event: AsyncEvent) {}

          def onTimeout(event: AsyncEvent) {
            withAsyncRequestAndResponse(event) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                renderHaltException(HaltException(Some(504), None, Map.empty, "Gateway timeout"))
                event.getAsyncContext.complete()
              }
            }
          }

          def onError(event: AsyncEvent) {}

          def onStartAsync(event: AsyncEvent) {}
        })

        f onResult {
          case a => {
            withAsyncRequestAndResponse(context) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                super.renderResponseBody(a)
                context.complete()
              }
            }
          }
        } recover {
          case t => {
            withAsyncRequestAndResponse(context) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                t match {
                  case e: HaltException => renderHaltException(e)
                  case e => errorHandler(e)
                }
                context.complete()
              }
            }
          }

        }
      }
      case a => {
        super.renderResponseBody(a)
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
  def asyncGet(routeMatchers: RouteMatcher*)(block: => Any): Route = addRoute(Get, routeMatchers, Future { block })

  /**
   * @see asyncGet
   */
  def asyncPost(routeMatchers: RouteMatcher*)(block: => Any): Route = addRoute(Post, routeMatchers, Future { block })

  /**
   * @see asyncGet
   */
  def asyncPut(routeMatchers: RouteMatcher*)(block: => Any): Route = addRoute(Put, routeMatchers, Future { block })

  /**
   * @see asyncGet
   */
  def asyncDelete(routeMatchers: RouteMatcher*)(block: => Any): Route = addRoute(Delete, routeMatchers, Future { block })

  /**
   * @see asyncGet
   */
  def asyncOptions(routeMatchers: RouteMatcher*)(block: => Any): Route = addRoute(Options, routeMatchers, Future { block })

  /**
   * @see asyncGet
   */
  def asyncPatch(routeMatchers: RouteMatcher*)(block: => Any): Route = addRoute(Patch, routeMatchers, Future { block })


}