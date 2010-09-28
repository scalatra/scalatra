package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.{ServletContext, ServletConfig, FilterConfig}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletTemplateEngine}

trait ScalateSupport extends ScalatraKernel {
  self: {
    def servletContext: ServletContext
  } =>

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
}
