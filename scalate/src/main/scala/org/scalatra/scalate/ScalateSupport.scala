package org.scalatra
package scalate

import scala.collection.mutable
import java.io.PrintWriter
import javax.servlet.{ServletContext, ServletConfig, FilterConfig}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.fusesource.scalate.{TemplateEngine, Binding, RenderContext}
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletTemplateEngine}
import org.fusesource.scalate.support.TemplateFinder

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

  private val TemplateAttributesKey = "org.scalatra.scalate.ScalateSupport.TemplateAttributes"
}

/**
 * ScalateSupport creates and configures a template engine and provides
 * helper methods and bindings to integrate with the ScalatraKernel.
 */
trait ScalateSupport extends ScalatraKernel {
  /**
   * The template engine used by the methods in this support class.  It
   * provides a lower-level interface to Scalate and may be used directly
   * to circumvent the conventions imposed by the helpers in this class.
   * For instance, paths passed directly to the template engine are not
   * run through `findTemplate`.
   */
  protected[scalatra] var templateEngine: TemplateEngine = _

  abstract override def initialize(config: Config) {
    super.initialize(config)
    templateEngine = createTemplateEngine(config)
  }

  /**
   * Creates the templateEngine from the config.  There is little reason to
   * override this unless you have created a ScalatraKernel extension outside
   * an HttpServlet or Filter.
   */
  protected def createTemplateEngine(config: Config): TemplateEngine =
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
    override def createRenderContext(uri: String, out: PrintWriter) = {
      val ctx = ScalateSupport.this.createRenderContext()
      ScalateSupport.this.templateAttributes foreach {
        case (name, value) => ctx.setAttribute(name, Some(value))
      }
      ctx
    }

    /**
     * Delegates to the ScalatraKernel's isDevelopmentMode flag.
     */
    override def isDevelopmentMode = ScalateSupport.this.isDevelopmentMode

    ScalateSupport.setLayoutStrategy(this)
    templateDirectories = defaultTemplatePath
    bindings ::= Binding("context", "_root_."+classOf[ScalatraRenderContext].getName, true, isImplicit = true)
    importStatements ::= "import org.scalatra.ServletApiImplicits._"
  }

  /**
   * Creates a render context to be used by default in the template engine.
   *
   * Returns a ScalatraRenderContext by default in order to bind some other
   * framework variables (e.g., multiParams, flash).  ScalatraTemplateEngine
   * assumes this returns ScalatraRenderContext in its binding of "context".
   * If you return something other than a ScalatraRenderContext, you will
   * also want to redefine that binding.
   */
  protected def createRenderContext(req: HttpServletRequest = request, resp: HttpServletResponse = response): RenderContext =
    new ScalatraRenderContext(this, req, resp)

  /**
   * Creates a render context and renders directly to that.  No template
   * search is performed, and the layout strategy is circumvented.  Clients
   * are urged to consider layoutTemplate instead.
   */
  @deprecated("not idiomatic Scalate; consider layoutTemplate instead", "2.0")
  def renderTemplate(path: String, attributes: (String, Any)*) =
    createRenderContext().render(path, Map(attributes : _*))

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

  /**
   * The default index page when the path is a directory.
   */
  protected def defaultIndexName: String = "index"

  /**
   * The default template format.
   */
  protected def defaultTemplateFormat: String = "scaml"

  /**
   * The default path to search for templates.  Left as a def so it can be
   * read from the servletContext in initialize, but you probably want a
   * constant.
   *
   * Defaults to:
   * - `/WEB-INF/views` (recommended)
   * - `/WEB-INF/scalate/templates` (used by previous Scalatra quickstarts)
   */
  protected def defaultTemplatePath: List[String] =
    List("/WEB-INF/views", "/WEB-INF/scalate/templates")

  /**
   * The default path to search for templates.  Left as a def so it can be
   * read from the servletContext in initialize, but you probably want a
   * constant.
   *
   * Defaults to:
   * - `/WEB-INF/views` (recommended)
   * - `/WEB-INF/scalate/templates` (used by previous Scalatra quickstarts)
   */
  protected def defaultLayoutPath: List[String] =
    List("/WEB-INF/views", "/WEB-INF/scalate/templates")

  /**
   * Convenience method for `layoutTemplateAs("jade")`.
   */
  protected def jade(path: String, attributes: (String, Any)*): String =
    layoutTemplateAs(Set("jade"))(path, attributes:_*)

  /**
   * Convenience method for `layoutTemplateAs("scaml")`.
   */
  protected def scaml(path: String, attributes: (String, Any)*): String =
    layoutTemplateAs(Set("scaml"))(path, attributes:_*)

  /**
   * Convenience method for `layoutTemplateAs("ssp")`.
   */
  protected def ssp(path: String, attributes: (String, Any)*): String =
    layoutTemplateAs(Set("ssp"))(path, attributes:_*)

  /**
   * Convenience method for `layoutTemplateAs("mustache")`.
   */
  protected def mustache(path: String, attributes: (String, Any)*): String =
    layoutTemplateAs(Set("mustache"))(path, attributes:_*)

  /**
   * Finds and renders a template with the current layout strategy,
   * returning the result.
   *
   * @param ext The extensions to look for a template.
   * @param path The path of the template, passed to `findTemplate`.
   * @param attributes Attributes to path to the render context.  Disable
   * layouts by passing `layout -> ""`.
   */
  protected def layoutTemplateAs(ext: Set[String])(path: String, attributes: (String, Any)*): String = {
    val uri = findTemplate(path, ext).getOrElse(path)
    templateEngine.layout(uri, Map(attributes:_*))
  }

  /**
   * Finds and renders a template with the current layout strategy,
   * looking for all known extensions, returning the result.
   *
   * @param ext The extension to look for a template.
   * @param path The path of the template, passed to `findTemplate`.
   * @param attributes Attributes to path to the render context.  Disable
   * layouts by passing `layout -> ""`.
   */
  protected def layoutTemplate(path: String, attributes: (String, Any)*): String =
    layoutTemplateAs(templateEngine.extensions)(path, attributes :_*)

  /**
   * Finds a template for a path.  Delegates to a TemplateFinder, and if
   * that fails, tries again with `/defaultIndexName` appended.
   */
  protected def findTemplate(path: String, extensionSet: Set[String] = templateEngine.extensions): Option[String] = {
    val finder = new TemplateFinder(templateEngine) {
      override lazy val extensions = extensionSet
    }
    finder.findTemplate("/"+path) orElse
      finder.findTemplate("/%s/%s".format(path, defaultIndexName))
  }

  /**
   * A request-scoped map of attributes to pass to the template.  This map
   * will be set to any render context created with the `createRenderContext`
   * method.
   */
  protected def templateAttributes: mutable.Map[String, Any] =
    request.getOrElseUpdate(ScalateSupport.TemplateAttributesKey, mutable.Map.empty).asInstanceOf[mutable.Map[String, Any]]
}
