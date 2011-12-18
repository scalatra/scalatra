package org.scalatra
package akka

import _root_.akka.actor.Actor
import _root_.akka.dispatch.Future
import _root_.akka.util.duration._
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

trait AkkaSupport extends AsyncSupport {
  import ScalatraKernel.Action
  
  override def asynchronously(f: => Any): Action = () => Future(f)

  override protected def renderResponseBody(actionResult: Any) = {
    actionResult match {
      case f: Future[_] => {
        val gotResponseAlready = new AtomicBoolean(false)
        val context = request.startAsync()
        context.setTimeout(f.timeoutInNanos.nanos.toMillis)
        context addListener (new AsyncListener {
          def onComplete(event: AsyncEvent) {}

          def onTimeout(event: AsyncEvent) {
            onAsyncEvent(event) {
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
            withinAsyncContext(context) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                super.renderResponseBody(a)
                context.complete()
              }
            }
          }
        } recover {
          case t => {
            withinAsyncContext(context) {
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
