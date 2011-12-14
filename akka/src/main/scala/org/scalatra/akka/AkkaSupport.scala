package org.scalatra
package akka

import _root_.akka.actor.Actor
import _root_.akka.dispatch.Future
import _root_.akka.util.duration._
import org.scalatra.ScalatraKernel
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.lang.{ Integer => JInteger }

trait ScalatraSupport { self: Actor =>
  /**
   * Immediately halts processing of a request.  Can be called from either a
   * before filter or a route.
   *
   * @param status the status to set on the response, or null to leave
   *        the status unchanged.
   * @param body a result to render through the render pipeline as the body
   * @param headers headers to add to the response
   * @param reason the HTTP status reason to set, or null to leave unchanged.
   */
  def halt[T : Manifest](status: JInteger = null,
           body: T = (),
           headers: Map[String, String] = Map.empty,
           reason: String = null): Nothing = {
    val statusOpt = if (status == null) None else Some(status.intValue)
    throw new HaltException(statusOpt, Some(reason), headers, body)
  }
}

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

}