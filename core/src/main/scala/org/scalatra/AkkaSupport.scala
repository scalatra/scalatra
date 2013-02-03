package org.scalatra

import _root_.akka.util.duration._
import _root_.akka.util.{Timeout, Duration}
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{ServletContext, AsyncEvent, AsyncListener}
import servlet.{ScalatraAsyncContext, ServletApiImplicits, AsyncSupport}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import _root_.akka.dispatch.{ExecutionContext, Future}
import util.{MapWithIndifferentAccess, MultiMapHeadView}
import java.util.concurrent.{Executors, ExecutorService}
import java.{util => ju}
import collection.immutable.DefaultMap
import collection.JavaConverters._
import org.scalatra.util.conversion.DefaultImplicitConversions


abstract class MinimalAsyncResult(implicit val asyncContext: ScalatraAsyncContext) extends ScalatraAsyncContext  {

  /* AsyncContextProxy */
  type ConfigT = asyncContext.ConfigT
  implicit val request: HttpServletRequest = asyncContext.request
  implicit val response: HttpServletResponse = asyncContext.response
  implicit val timeout: Timeout = asyncContext.timeout
  val config: ConfigT = asyncContext.config
  /* end AsyncContextProxy */

  def is: Future[_]
}

trait FutureSupport extends AsyncSupport[MinimalAsyncResult] {

  implicit protected def executor: ExecutionContext

  override def asynchronously(f: ⇒ Any): Action = () ⇒ Future(f)

  // Still thinking of the best way to specify this before making it public.
  // In the meantime, this gives us enough control for our test.
  // IPC: it may not be perfect but I need to be able to configure this timeout in an application
  protected def asyncTimeout: Duration = 30 seconds


  override protected def isAsyncExecutable(result: Any) = classOf[Future[_]].isAssignableFrom(result.getClass)

  implicit protected def asyncContext(implicit executor: ExecutionContext): AsyncContext = new ScalatraAsyncContext {
    type ConfigT = FutureSupport.this.ConfigT
    val config: ConfigT = FutureSupport.this.config
    implicit val request: HttpServletRequest = FutureSupport.this.request
    implicit val timeout: Timeout = Timeout(asyncTimeout)
    implicit val response: HttpServletResponse =  FutureSupport.this.response
  }


  override protected def renderResponse(actionResult: Any) {
    actionResult match {
      case r: MinimalAsyncResult => renderResponse(r.is)
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

trait DefaultAsyncContext { self: FutureSupport =>
  type AsyncContext = ScalatraContext
  type AsyncResult = MinimalAsyncResult
}

class MyApp extends ScalatraServlet with FutureSupport with DefaultAsyncContext {

  get("/foo") {
    new AsyncResult { def is =
      Future {
        "hey"
      }
    }
  }
}