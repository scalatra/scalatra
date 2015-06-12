package org.scalatra

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import javax.servlet.{ AsyncEvent, AsyncListener, ServletContext }

import org.scalatra.servlet.AsyncSupport

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

@deprecated("AsyncResult is no longer required and will be removed in a coming release.", "2.4.0")
abstract class AsyncResult(
  implicit override val scalatraContext: ScalatraContext)
    extends ScalatraContext {

  implicit val request: HttpServletRequest = scalatraContext.request

  implicit val response: HttpServletResponse = scalatraContext.response

  val servletContext: ServletContext = scalatraContext.servletContext

  // This is a Duration instead of a timeout because a duration has the concept of infinity
  implicit def timeout: Duration = 30 seconds

  val is: Future[_]

}

trait FutureSupport extends AsyncSupport {

  implicit protected def executor: ExecutionContext

  override def asynchronously(f: => Any): Action = () => Future(f)

  // Still thinking of the best way to specify this before making it public.
  // In the meantime, this gives us enough control for our test.
  // IPC: it may not be perfect but I need to be able to configure this timeout in an application
  // This is a Duration instead of a timeout because a duration has the concept of infinity
  // @deprecated("Override the `timeout` method on a `org.scalatra.AsyncResult` instead.", "2.2")
  protected def asyncTimeout: Duration = 30 seconds

  override protected def isAsyncExecutable(result: Any): Boolean =
    classOf[Future[_]].isAssignableFrom(result.getClass) ||
      classOf[AsyncResult].isAssignableFrom(result.getClass)

  override protected def renderResponse(actionResult: Any): Unit = {
    actionResult match {
      case r: AsyncResult => handleFuture(r.is, r.timeout)
      case f: Future[_] => handleFuture(f, asyncTimeout)
      case a => super.renderResponse(a)
    }
  }

  private[this] def handleFuture(f: Future[_], timeout: Duration): Unit = {
    val gotResponseAlready = new AtomicBoolean(false)
    val context = request.startAsync(request, response)
    if (timeout.isFinite()) context.setTimeout(timeout.toMillis) else context.setTimeout(-1)

    def renderFutureResult(f: Future[_]): Unit = {
      f onComplete {
        // Loop until we have a non-future result
        case Success(f2: Future[_]) => renderFutureResult(f2)
        case Success(r: AsyncResult) => renderFutureResult(r.is)
        case t => {

          if (gotResponseAlready.compareAndSet(false, true)) {
            withinAsyncContext(context) {
              try {
                t map { result =>
                  renderResponse(result)
                } recover {
                  case e: HaltException =>
                    renderHaltException(e)
                  case e =>
                    try {
                      renderResponse(errorHandler(e))
                    } catch {
                      case e: Throwable =>
                        ScalatraBase.runCallbacks(Failure(e))
                        renderUncaughtException(e)
                        ScalatraBase.runRenderCallbacks(Failure(e))
                    }
                }
              } finally {
                context.complete()
              }
            }
          }
        }
      }
    }

    context addListener new AsyncListener {

      def onTimeout(event: AsyncEvent): Unit = {
        onAsyncEvent(event) {
          if (gotResponseAlready.compareAndSet(false, true)) {
            renderHaltException(HaltException(Some(504), None, Map.empty, "Gateway timeout"))
            event.getAsyncContext.complete()
          }
        }
      }

      def onComplete(event: AsyncEvent): Unit = {}

      def onError(event: AsyncEvent): Unit = {
        onAsyncEvent(event) {
          if (gotResponseAlready.compareAndSet(false, true)) {
            event.getThrowable match {
              case e: HaltException => renderHaltException(e)
              case e =>
                try {
                  renderResponse(errorHandler(e))
                } catch {
                  case e: Throwable =>
                    ScalatraBase.runCallbacks(Failure(e))
                    renderUncaughtException(e)
                    ScalatraBase.runRenderCallbacks(Failure(e))
                }
            }
          }
        }
      }

      def onStartAsync(event: AsyncEvent): Unit = {}
    }

    renderFutureResult(f)
  }

}

