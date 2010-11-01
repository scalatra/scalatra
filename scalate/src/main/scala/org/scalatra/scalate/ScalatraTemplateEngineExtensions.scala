package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.{FilterConfig, ServletConfig}
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.ServletTemplateEngine

/**
 * This used to be an inner trait of ScalateSupport, but we need a class name without '$' for logback.
 * See http://github.com/scalatra/scalatra/issues#issue/11.  If this issue is fixed in a future version
 * of Logback or Scalate, then this trait will probably move back into ScalateSupport.
 */
private trait ScalatraTemplateEngineExtensions extends TemplateEngine {
  protected def scalateSupport: ScalateSupport
  override def createRenderContext(uri: String, out: PrintWriter) = scalateSupport.createRenderContext
  override def isDevelopmentMode = scalateSupport.isDevelopmentMode
}

// Implementations of ScalatraTemplateEngineExtensions to give a '$'-less name to appease Logback.

private class ScalatraTemplateEngine(val scalateSupport: ScalateSupport)
  extends TemplateEngine with ScalatraTemplateEngineExtensions

private class ScalatraServletTemplateEngine(val scalateSupport: ScalateSupport, config: ServletConfig)
  extends ServletTemplateEngine(config) with ScalatraTemplateEngineExtensions

private class ScalatraFilterTemplateEngine(val scalateSupport: ScalateSupport, config: FilterConfig)
  extends ServletTemplateEngine(config) with ScalatraTemplateEngineExtensions
