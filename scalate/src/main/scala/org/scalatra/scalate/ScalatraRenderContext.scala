package org.scalatra
package scalate

import java.io.PrintWriter
import org.fusesource.scalate.{AttributeMap, RenderContext, DefaultRenderContext, TemplateEngine}
import collection.mutable

object ScalatraRenderContext {
  /**
   * Returns the currently active render context in this thread
   * @throws IllegalArgumentException if there is no suitable render context available in this thread
   */
  def renderContext: ScalatraRenderContext = RenderContext() match {
    case s: ScalatraRenderContext => s
    case n => throw new IllegalArgumentException("This threads RenderContext is not a ScalatraRenderContext as it is: " + n)
  }

  def request: HttpRequest = renderContext.request

  def response: HttpResponse = renderContext.response

  implicit def appContext: AppContext = renderContext.appContext

}

/**
 * A render context integrated with Scalatra.  Exposes a few extra
 * standard bindings to the template.
 */
class ScalatraRenderContext(
    protected val kernel: ScalatraApp,
    engine: TemplateEngine,
    out: PrintWriter,
    val request: HttpRequest,
    val response: HttpResponse)(implicit val appContext: AppContext)
  extends DefaultRenderContext(request.uri.toASCIIString, engine, out)
{
  def this(scalate: ScalateSupport, request: HttpRequest, response: HttpResponse)(implicit appContext: AppContext) = this(scalate, scalate.templateEngine, response.writer, request, response)

  def this(scalate: ScalateSupport)(implicit appContext: AppContext) = this(scalate, scalate.request, scalate.response)

  def flash: scala.collection.Map[String, Any] = kernel match {
    case flashMapSupport: FlashMapSupport => flashMapSupport.flash
    case _ => Map.empty
  }

  def session: HttpSession = kernel match {
    case sess: SessionSupport => sess.session
    case _ => new NoopHttpSession
  }

  def cookies = kernel.cookies

  def sessionOption: Option[HttpSession] = kernel match {
    case sess: SessionSupport => sess.sessionOption
    case _ => None
  }

  def params: Map[String, String] = kernel.params

  def multiParams: MultiParams = kernel.multiParams

  override def locale = kernel.locale

  viewPrefixes = List("WEB-INF", "")

  override val attributes = new AttributeMap {
    request += "context" -> ScalatraRenderContext.this

    def get(key: String): Option[Any] = request.get(key)

    def apply(key: String): Any = key match {
      case "context" => ScalatraRenderContext.this
      case _ => get(key).orNull
    }

    def update(key: String, value: Any): Unit = value match {
      case null => request -= key
      case _ => request += key -> value
    }

    def remove(key: String) = {
      val answer = get(key)
      request -= key
      answer
    }

    def keySet: Set[String] = {
      val answer = new mutable.HashSet[String]()
      for (a <- request.keysIterator) {
        answer.add(a.toString)
      }
      answer.toSet
    }

    override def toString = keySet.map(k => "" + k + " -> " + apply(k)).mkString("{", ", ", "}")


  }


}
