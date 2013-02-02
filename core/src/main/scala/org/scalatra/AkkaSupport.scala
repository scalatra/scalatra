package org.scalatra

import _root_.akka.util.duration._
import _root_.akka.util.{Timeout, Duration}
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{ServletContext, AsyncEvent, AsyncListener}
import servlet.{ServletApiImplicits, AsyncSupport}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import _root_.akka.dispatch.{ExecutionContext, Future}
import util.{MapWithIndifferentAccess, MultiMapHeadView}
import java.util.concurrent.{Executors, ExecutorService}
import java.{util => ju}
import collection.immutable.DefaultMap
import collection.JavaConverters._
import org.scalatra.util.conversion.DefaultImplicitConversions

trait ScalatraAsyncContext extends ServletApiImplicits {
  type ConfigT <: {
    def getServletContext(): ServletContext
    def getInitParameter(name: String): String
    def getInitParameterNames(): ju.Enumeration[String]
  }
  implicit def request: HttpServletRequest
  implicit def response: HttpServletResponse
  implicit def timeout: Timeout
  def config: ConfigT

  trait Config {
    def context: ServletContext
    def initParameters: Map[String, String]
  }
  protected implicit def configWrapper(config: ConfigT): Config =  new Config {
    def context = config.getServletContext

    object initParameters extends DefaultMap[String, String] {
      def get(key: String): Option[String] = Option(config.getInitParameter(key))

      def iterator: Iterator[(String, String)] =
        for (name <- config.getInitParameterNames.asScala.toIterator)
          yield (name, config.getInitParameter(name))
    }
  }

  /**
   * Gets an init paramter from the config.
   *
   * @param name the name of the key
   *
   * @return an option containing the value of the parameter if defined, or
   * `None` if the parameter is not set.
   */
  def initParameter(name: String): Option[String] = config.initParameters.get(name)

  /**
   * The servlet context in which this kernel runs.
   */
  def servletContext: ServletContext = config.context

  /**
   * A free form string representing the environment.
   * `org.scalatra.Environment` is looked up as a system property, and if
   * absent, and init parameter.  The default value is `development`.
   */
  def environment: String = System.getProperty(EnvironmentKey, initParameter(EnvironmentKey).getOrElse("DEVELOPMENT"))

  /**
   * A boolean flag representing whether the kernel is in development mode.
   * The default is true if the `environment` begins with "dev", case-insensitive.
   */
  def isDevelopmentMode = environment.toUpperCase.startsWith("DEV")

  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  def multiParams: MultiParams = {
    val read = request.contains("MultiParamsRead")
    val found = request.get(MultiParamsKey) map (
      _.asInstanceOf[MultiParams] ++ (if (read) Map.empty else request.multiParameters)
    )
    val multi = found getOrElse request.multiParameters
    request("MultiParamsRead") = new {}
    request(MultiParamsKey) = multi
    multi.withDefaultValue(Seq.empty)
  }

  /*
   * Assumes that there is never a null or empty value in multiParams.  The servlet container won't put them
   * in request.getParameters, and we shouldn't either.
   */
  protected val _params: Params = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }
}
abstract class MinimalAsyncResult(implicit val asyncContext: ScalatraAsyncContext) extends ScalatraAsyncContext with ScalatraParamsImplicits with DefaultImplicitConversions {

  /* AsyncContextProxy */
  type ConfigT = asyncContext.ConfigT
  implicit val request: HttpServletRequest = asyncContext.request
  implicit val response: HttpServletResponse = asyncContext.response
  implicit val timeout: Timeout = asyncContext.timeout
  val config: ConfigT = asyncContext.config
  /* end AsyncContextProxy */

  def is: Future[_]
}

trait AkkaSupport extends AsyncSupport[MinimalAsyncResult] {

  implicit protected def executor: ExecutionContext

  override def asynchronously(f: ⇒ Any): Action = () ⇒ Future(f)

  // Still thinking of the best way to specify this before making it public.
  // In the meantime, this gives us enough control for our test.
  // IPC: it may not be perfect but I need to be able to configure this timeout in an application
  protected def asyncTimeout: Duration = 30 seconds


  override protected def isAsyncExecutable(result: Any) = classOf[Future[_]].isAssignableFrom(result.getClass)

  implicit protected def asyncContext(implicit executor: ExecutionContext): AsyncContext = new ScalatraAsyncContext {
    type ConfigT = AkkaSupport.this.ConfigT
    val config: ConfigT = AkkaSupport.this.config
    implicit val request: HttpServletRequest = AkkaSupport.this.request
    implicit val timeout: Timeout = Timeout(asyncTimeout)
    implicit val response: HttpServletResponse =  AkkaSupport.this.response
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

trait DefaultAsyncContext { self: AkkaSupport =>

  type AsyncContext = ScalatraAsyncContext
  type AsyncResult = MinimalAsyncResult

}

class MyApp extends ScalatraServlet with AkkaSupport with DefaultAsyncContext {

  get("/foo") {
    new AsyncResult { def is =
      Future {
        "hey"
      }
    }
  }
}