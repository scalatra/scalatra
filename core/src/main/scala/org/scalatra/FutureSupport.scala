package org.scalatra

import java.util.concurrent.atomic.AtomicBoolean
import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}
import org.scalatra.ServletCompat.{AsyncEvent, AsyncListener, ServletContext}

import org.scalatra.servlet.AsyncSupport

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

abstract class AsyncResult(implicit override val scalatraContext: ScalatraContext) extends ScalatraContext {

  implicit val request: HttpServletRequest = scalatraContext.request

  implicit val response: HttpServletResponse = scalatraContext.response

  val servletContext: ServletContext = scalatraContext.servletContext

  // This is a Duration instead of a timeout because a duration has the concept of infinity
  implicit def timeout: Duration = 30.seconds

  val is: Future[?]

}

trait FutureSupport extends AsyncSupport {

  implicit protected def executor: ExecutionContext

  override def asynchronously(f: => Any): Action = () => Future(f)

  override protected def isAsyncExecutable(result: Any): Boolean =
    classOf[Future[?]].isAssignableFrom(result.getClass) ||
      classOf[AsyncResult].isAssignableFrom(result.getClass)

  override protected def renderResponse(actionResult: Any): Unit = {
    actionResult match {
      case r: AsyncResult => handleFuture(r.is, Some(r.timeout))
      case f: Future[?]   => handleFuture(f, None)
      case a              => super.renderResponse(a)
    }
  }

  private[this] def handleFuture(f: Future[?], timeout: Option[Duration]): Unit = {
    val gotResponseAlready = new AtomicBoolean(false)
    val context            = request.startAsync(request, response)
    timeout.foreach { timeout =>
      if (timeout.isFinite) context.setTimeout(timeout.toMillis) else context.setTimeout(-1)
    }

    def renderFutureResult(f: Future[?]): Unit = {
      f onComplete {
        // Loop until we have a non-future result
        case Success(f2: Future[?])  => renderFutureResult(f2)
        case Success(r: AsyncResult) => renderFutureResult(r.is)
        case t                       => {

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
                      case e: HaltException =>
                        renderHaltException(e)
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
            renderHaltException(HaltException(Some(504), Map.empty, "Gateway timeout"))
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
              case e                =>
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
