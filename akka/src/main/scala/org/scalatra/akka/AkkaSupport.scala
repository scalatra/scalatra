package org.scalatra
package akka

import _root_.akka.util.duration._
import _root_.akka.util.Duration
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import _root_.akka.dispatch.{ExecutionContext, Await, Future}
import _root_.akka.actor.{Actor, ActorSystem}
import servlet.AsyncSupport

trait AkkaSupport extends AsyncSupport {
  protected type ExecutionContext = _root_.akka.dispatch.ExecutionContext

  protected implicit def defaultExecutor(implicit system: ActorSystem): ExecutionContext =
    ExecutionContext.defaultExecutionContext(system)

  override def asynchronously(f: ⇒ Any)(implicit executor: ExecutionContext): Action = () ⇒ Future(f)

  // Still thinking of the best way to specify this before making it public. 
  // In the meantime, this gives us enough control for our test.
  // IPC: it may not be perfect but I need to be able to configure this timeout in an application
  protected def asyncTimeout: Duration = 30 seconds


  override protected def isAsyncExecutable(result: Any) = result match {
    case _: Future[_] => true
    case _ => false
  }

  override protected def renderResponse(actionResult: Any) = {

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

        f onSuccess {
          case a ⇒ {
            withinAsyncContext(context) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                runFilters(routes.afterFilters)
                super.renderResponse(a)

                context.complete()
              }
            }
          }
        } onFailure {
          case t ⇒ {
            withinAsyncContext(context) {
              if (gotResponseAlready.compareAndSet(false, true)) {
                t match {
                  case e: HaltException ⇒ renderHaltException(e)
                  case e ⇒ renderResponse(errorHandler(e))
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
