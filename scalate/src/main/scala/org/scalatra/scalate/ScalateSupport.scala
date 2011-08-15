package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.{ServletContext, ServletConfig, FilterConfig}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletTemplateEngine}
import java.lang.Throwable

trait ScalateSupport extends ScalatraKernel {
  protected var templateEngine: TemplateEngine = _

  abstract override def initialize(config: Config) {
    super.initialize(config)
    templateEngine = createTemplateEngine(config)
  }

  protected def createTemplateEngine(config: Config) =
    config match {
      case servletConfig: ServletConfig =>
        new ServletTemplateEngine(servletConfig) with ScalatraTemplateEngine
      case filterConfig: FilterConfig =>
        new ServletTemplateEngine(filterConfig) with ScalatraTemplateEngine
      case _ =>
        // Don't know how to convert your Config to something that
        // ServletTemplateEngine can accept, so fall back to a TemplateEngine
        new TemplateEngine with ScalatraTemplateEngine
    }

  trait ScalatraTemplateEngine {
    this: TemplateEngine =>

    override def createRenderContext(uri: String, out: PrintWriter) =
      ScalateSupport.this.createRenderContext

    override def isDevelopmentMode = ScalateSupport.this.isDevelopmentMode
  }

  def createRenderContext: ServletRenderContext =
    new ServletRenderContext(templateEngine, request, response, servletContext)

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
    val renderContext =
      new ServletRenderContext(templateEngine, req, resp, servletContext)
    renderContext.setAttribute("javax.servlet.error.exception", Some(e))
    templateEngine.layout(errorPage, renderContext)
  }
}
