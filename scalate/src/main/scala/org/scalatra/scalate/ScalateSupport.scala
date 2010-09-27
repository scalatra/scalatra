package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.{ServletContext, ServletConfig, FilterConfig}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletTemplateEngine}

trait ScalateSupport extends Initializable {
  self: {
    def servletContext: ServletContext
    def request: HttpServletRequest
    def response: HttpServletResponse
  } =>

  protected var templateEngine: TemplateEngine = _

  abstract override def initialize(config: Config) {
    super.initialize(config)
    templateEngine = createTemplateEngine(config)
  }

  protected def createTemplateEngine(config: Config) = 
    config match {
      case servletConfig: ServletConfig =>
        new ServletTemplateEngine(servletConfig) with CreatesServletRenderContext
      case filterConfig: FilterConfig =>
        new ServletTemplateEngine(filterConfig) with CreatesServletRenderContext
      case _ =>
        // Don't know how to convert your Config to something that
        // ServletTemplateEngine can accept, so fall back to a TemplateEngine
        new TemplateEngine with CreatesServletRenderContext
    }

  trait CreatesServletRenderContext {
    this: TemplateEngine =>

    override def createRenderContext(uri: String, out: PrintWriter) =
      ScalateSupport.this.createRenderContext
  }

  def createRenderContext: ServletRenderContext = 
    new ServletRenderContext(templateEngine, request, response, servletContext)

  def renderTemplate(path: String, attributes: (String, Any)*) = 
    createRenderContext.render(path, Map(attributes : _*))
}
