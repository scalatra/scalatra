package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.{ServletContext, ServletConfig, FilterConfig}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.fusesource.scalate.{TemplateEngine, Binding}
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletTemplateEngine}
import java.lang.Throwable

object ScalateSupport {
  val DefaultLayouts = Seq(
    "/WEB-INF/layouts/default",
    "/WEB-INF/scalate/layouts/default"
  )
  private def setLayoutStrategy(engine: TemplateEngine) = {
    val layouts = for {
      base <- ScalateSupport.DefaultLayouts
      extension <- TemplateEngine.templateTypes
    } yield ("%s.%s".format(base, extension))
    engine.layoutStrategy = new DefaultLayoutStrategy(engine, layouts:_*)
  }
}

trait ScalateSupport extends ScalatraKernel {
  protected var templateEngine: TemplateEngine = _

  abstract override def initialize(config: Config) {
    super.initialize(config)
    templateEngine = createTemplateEngine(config)
  }

  protected def createTemplateEngine(config: Config) = {
    val engine = config match {
      case servletConfig: ServletConfig =>
        new ServletTemplateEngine(servletConfig) with ScalatraTemplateEngine
      case filterConfig: FilterConfig =>
        new ServletTemplateEngine(filterConfig) with ScalatraTemplateEngine
      case _ =>
        // Don't know how to convert your Config to something that
        // ServletTemplateEngine can accept, so fall back to a TemplateEngine
        new TemplateEngine with ScalatraTemplateEngine
    }
    engine.bindings = engine.bindings ::: List(
      Binding("servlet", this.getClass.getName, true),
      Binding("urlGenerator", classOf[UrlGeneratorSupport].getName, true)
    )

    engine
  }

  /**
   * A TemplateEngine integrated with Scalatra.
   *
   * A ScalatraTemplateEngine looks for layouts in `/WEB-INF/layouts` before
   * searching the default `/WEB-INF/scalate/layouts`.
   */
  trait ScalatraTemplateEngine {
    this: TemplateEngine =>

    /**
     * Returns a ServletRenderContext constructed from the current
     * request and response.
     */
    override def createRenderContext(uri: String, out: PrintWriter) =
      ScalateSupport.this.createRenderContext

    /**
     * Delegates to the ScalatraKernel's isDevelopmentMode flag.
     */
    override def isDevelopmentMode = ScalateSupport.this.isDevelopmentMode

    ScalateSupport.setLayoutStrategy(this)
  }

  def createRenderContext: ServletRenderContext =
    createRenderContext(request, response)

  def createRenderContext(req: HttpServletRequest, resp: HttpServletResponse): ServletRenderContext = {
    val context = new ServletRenderContext(templateEngine, req, resp, servletContext)
    context.attributes.update("servlet", this)
    context.attributes.update("urlGenerator", UrlGenerator)
    context
  }

  def renderTemplate(path: String, attributes: (String, Any)*) =
    createRenderContext.render(path, Map(attributes : _*))

  /**
   * Flag whether the Scalate error page is enabled.  If true, uncaught
   * exceptions will be caught and rendered by the Scalate error page.
   *
   * The default is true.
   */
  protected def isScalateErrorPageEnabled = true

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    try {
      super.handle(req, res)
    }
    catch {
      case e if isScalateErrorPageEnabled => renderScalateErrorPage(req, res, e)
      case e => throw e
    }
  }

  // Hack: Have to pass it the request and response, because we're outside the
  // scope of the super handler.
  private def renderScalateErrorPage(req: HttpServletRequest, resp: HttpServletResponse, e: Throwable) = {
    resp.setContentType("text/html")
    val errorPage = templateEngine.load("/WEB-INF/scalate/errors/500.scaml")
    val context = createRenderContext(req, resp)
    context.setAttribute("javax.servlet.error.exception", Some(e))
    templateEngine.layout(errorPage, context)
  }
}
