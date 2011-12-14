package org.scalatra
package akka

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

          def onError(event: AsyncEvent) {
            withAsyncRequestAndResponse(event) {
              if (gotResponseAlready.compareAndSet(false, true))
                event.getThrowable match {
                  case e: HaltException => renderHaltException(e)
                  case e => errorHandler(e)
                }
            }
          }

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
        }

      }
      case a => {
        super.renderResponseBody(a)
      }
    }
  }

}