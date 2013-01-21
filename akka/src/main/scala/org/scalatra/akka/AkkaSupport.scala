package org.scalatra
package akka

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import _root_.akka.actor.{Actor, ActorSystem}
import servlet.AsyncSupport
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

trait AkkaSupport extends AsyncSupport {
  implicit protected def executor: ExecutionContext

  // Still thinking of the best way to specify this before making it public.
  // In the meantime, this gives us enough control for our test.
  // IPC: it may not be perfect but I need to be able to configure this timeout in an application
  protected def asyncTimeout: Duration = 30 seconds


  override protected def isAsyncExecutable(result: Any) = result match {
    case _: Future[_] => true
    case _ => false
  }

  override protected def renderResponse(actionResult: Any)(implicit ctx: ActionContext) = {

    actionResult match {
      case f: Future[_] ⇒ {
        val gotResponseAlready = new AtomicBoolean(false)
        val context = request.startAsync()
        context.setTimeout(asyncTimeout.toMillis)
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

        f onComplete {
          case t ⇒ {
            withinAsyncContext(context) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                t.map { result =>
                  runFilters(routes.afterFilters)
                  super.renderResponse(result)
                }.recover {
                  case e: HaltException ⇒ renderHaltException(e)
                  case e => renderResponse(errorHandler(e))
                }
                context.complete()
              }
            }
          }
        }
      }
      case a ⇒ {
        super.renderResponse(a)
      }
    }
  }
}
