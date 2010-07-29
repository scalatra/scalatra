package org.scalatra.scalate

import scala.tools.nsc.Global
import java.io.{File, PrintWriter}
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.fusesource.scalate.{Binding, RenderContext, TemplateEngine}
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletResourceLoader, ServletTemplateEngine}
import org.fusesource.scalate.util.ClassPathBuilder

trait ScalateSupport {
  self: {
    def servletContext: ServletContext
    def request: HttpServletRequest
    def response: HttpServletResponse
  } =>

  // Laziness lets the servlet context initialize itself first.
  //
  // TODO Convert to a org.fusesource.scalate.servlet.ServletTemplateEngine.  
  // Currently, ServletTemplateEngine can't be constructed from a Filter.
  // Going to send them a patch.  In the meantime, we'll just borrow their
  // implementation.
  protected lazy val templateEngine = new TemplateEngine {
    bindings = List(Binding("context", classOf[ServletRenderContext].getName, true, isImplicit = true))  

    // If the scalate.workingdir is not set, then just configure the working
    // directory under WEB_INF/_scalate
    if ( System.getProperty("scalate.workingdir", "").length == 0 ) {
      val path = servletContext.getRealPath("WEB-INF")
      if (path != null) {
        workingDirectory = new File(path, "_scalate")
      }
    }
  
    classpath = buildClassPath
    resourceLoader = new ServletResourceLoader(servletContext)
    layoutStrategy = new DefaultLayoutStrategy(this, TemplateEngine.templateTypes.map("/WEB-INF/scalate/layouts/default." + _):_*)

    private def buildClassPath(): String = {

      val builder = new ClassPathBuilder

      // Add containers class path
      builder.addPathFrom(getClass)
              .addPathFrom(classOf[ServletContext])
              .addPathFrom(classOf[Product])
              .addPathFrom(classOf[Global])

      // Always include WEB-INF/classes and all the JARs in WEB-INF/lib just in case
      builder.addClassesDir(servletContext.getRealPath("/WEB-INF/classes"))
              .addLibDir(servletContext.getRealPath("/WEB-INF/lib"))

      builder.classPath
    }

    override def createRenderContext(out: PrintWriter) =
      ScalateSupport.this.createRenderContext(request, response)
  }

  def createRenderContext(implicit req: HttpServletRequest, res: HttpServletResponse): ServletRenderContext = 
    new ServletRenderContext(templateEngine, req, res, servletContext)

  def renderTemplate(path: String, attributes: (String, Any)*)
                    (implicit request: HttpServletRequest, response: HttpServletResponse) = 
    createRenderContext.render(path, Map(attributes : _*))
}
