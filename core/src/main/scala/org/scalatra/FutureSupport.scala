package org.scalatra

import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{ServletContext, AsyncEvent, AsyncListener}
//import servlet.AsyncSupport
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class AsyncResult(implicit sc: ScalatraContext) extends ScalatraContext  {

  override protected def scalatraContext(implicit req: HttpServletRequest, resp: HttpServletResponse) = sc

//  implicit val request: HttpServletRequest = sc.request
//  implicit val response: HttpServletResponse = sc.response
  val servletContext: ServletContext = sc.servletContext

  // This is a Duration instead of a timeout because a duration has the concept of infinity
  implicit def timeout: Duration = 30 seconds
  val is: Future[_]
}

trait FutureSupport extends ScalatraBase {

  implicit protected def executor: ExecutionContext

//  override def asynchronously(f: ⇒ Any): Action = () ⇒ Future(f)

  // Still thinking of the best way to specify this before making it public.
  // In the meantime, this gives us enough control for our test.
  // IPC: it may not be perfect but I need to be able to configure this timeout in an application
  // This is a Duration instead of a timeout because a duration has the concept of infinity
  @deprecated("Override the `timeout` method on a `org.scalatra.AsyncResult` instead.", "2.2")
  protected def asyncTimeout: Duration = 30 seconds


  override protected def isAsyncExecutable(result: Any) =
    classOf[Future[_]].isAssignableFrom(result.getClass) ||
      classOf[AsyncResult].isAssignableFrom(result.getClass)

  override protected def renderResponse(req: HttpServletRequest, resp: HttpServletResponse, actionResult: Any) {
    actionResult match {
      case r: AsyncResult ⇒ handleFuture(req, resp, r.is , r.timeout)
      case f: Future[_]   ⇒ handleFuture(req, resp, f, asyncTimeout)
      case a              ⇒ super.renderResponse(req, resp, a)
    }
  }

  private[this] def handleFuture(req: HttpServletRequest, resp: HttpServletResponse, f: Future[_], timeout: Duration) {
    val gotResponseAlready = new AtomicBoolean(false)
    val context = req.startAsync(req, resp)
    if (timeout.isFinite()) context.setTimeout(timeout.toMillis) else context.setTimeout(-1)

    def renderFutureResult(f: Future[_]) {
      f onComplete {
        // Loop until we have a non-future result
        case Success(f2: Future[_]) => renderFutureResult(f2)
        case Success(r: AsyncResult) => renderFutureResult(r.is)
        case t ⇒ {
//          withinAsyncContext(context) {
            if (gotResponseAlready.compareAndSet(false, true)) {
              try {
                t map { result ⇒
                  renderResponse(req, resp, result)
                } recover {
                  case e: HaltException ⇒
                    renderHaltException(req, resp, e)
                  case e ⇒
                    try {
                      renderResponse(req, resp, errorHandler(e))
                    } catch {
                      case e: Throwable =>
                        ScalatraBase.runCallbacks(req, Failure(e))
                        renderUncaughtException(req, resp, e)
                        ScalatraBase.runRenderCallbacks(req, Failure(e))
                    }
                }
              } finally {
                context.complete()
              }
            }
//          }
        }
      }
    }

    context addListener new AsyncListener {
      def onTimeout(event: AsyncEvent) {
//        onAsyncEvent(event) {
          if (gotResponseAlready.compareAndSet(false, true)) {
            renderHaltException(req, resp, HaltException(Some(504), None, Map.empty, "Gateway timeout"))
            event.getAsyncContext.complete()
          }
//        }
      }
      def onComplete(event: AsyncEvent) {}
      def onError(event: AsyncEvent) {
//        onAsyncEvent(event) {
          if (gotResponseAlready.compareAndSet(false, true)) {
            event.getThrowable match {
              case e: HaltException => renderHaltException(req, resp, e)
              case e =>
                try {
                  renderResponse(req, resp, errorHandler(e))
                } catch {
                  case e: Throwable =>
                    ScalatraBase.runCallbacks(req, Failure(e))
                    renderUncaughtException(req, resp, e)
                    ScalatraBase.runRenderCallbacks(req, Failure(e))
                }
            }
          }
//        }
      }
      def onStartAsync(event: AsyncEvent) {}
    }

    renderFutureResult(f)
  }
}




