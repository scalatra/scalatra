package com.thinkminimo.step.scalate

import java.io.{StringWriter, PrintWriter}
import javax.servlet.ServletContext
import org.fusesource.scalate.{DefaultRenderContext, Template, TemplateEngine}
import org.fusesource.scalate.servlet.ServletResourceLoader

trait ScalateSupport {
  self: {def servletContext: ServletContext} =>

  // Laziness lets the servlet context initialize itself first.
  private lazy val templateEngine = {
    val result = new TemplateEngine
    result.resourceLoader = new ServletResourceLoader(servletContext)
    result
  }

  def renderTemplate(templateName: String, variables: (String, Any)*): java.io.StringWriter = {
    val template = templateEngine.load(templateName)
    val buffer = new StringWriter
    val context = new DefaultRenderContext(templateEngine, new PrintWriter(buffer))
    for (variable <- variables) {
      val (key, value) = variable
      context.attributes(key) = value
    }
    template.render(context)
    buffer
  }
}