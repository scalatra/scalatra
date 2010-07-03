package org.scalatra.scalate

import javax.servlet.ServletContext
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletResourceLoader}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

trait ScalateSupport {
  self: {def servletContext: ServletContext} =>

  // Laziness lets the servlet context initialize itself first.
  private lazy val templateEngine = {
    val result = new TemplateEngine
    result.resourceLoader = new ServletResourceLoader(servletContext)
    result
  }

  def renderTemplate(path: String, attributes: (String, Any)*)
                    (implicit request: HttpServletRequest, response: HttpServletResponse) {
    new ServletRenderContext(templateEngine, request, response, servletContext).render(path, Map(attributes : _*))
  }
}