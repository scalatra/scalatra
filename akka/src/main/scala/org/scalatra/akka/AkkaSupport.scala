package org.scalatra
package akka

import _root_.akka.util.duration._
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{ AsyncContext, AsyncEvent, AsyncListener }
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import _root_.akka.dispatch.{ Await, Future }
import _root_.akka.actor.{ Actor, ActorSystem }
import servlet.AsyncSupport

trait AkkaSupport extends AsyncSupport {
  implicit protected def system: ActorSystem

  protected def akkaDispatcherName: Option[String] = None

  private implicit lazy val _executor = akkaDispatcherName map system.dispatchers.lookup getOrElse system.dispatcher
  override def asynchronously(f: ⇒ Any): Action = () ⇒ Future(f)

  override protected def renderResponseBody(actionResult: Any) = {
    actionResult match {
      case f: Future[_] ⇒ {
        val gotResponseAlready = new AtomicBoolean(false)
        val context = request.startAsync()
        context.setTimeout(5000L)
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
                super.renderResponseBody(a)
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
                  case e                ⇒ renderResponseBody(errorHandler(e))
                }
                context.complete()
              }
            }
          }
        }
      }
      case a ⇒ {
        super.renderResponseBody(a)
      }
    }
  }
}
