package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import javax.servlet.ServletContext

import org.fusesource.scalate.{ Binding, TemplateEngine }
import org.fusesource.scalate.servlet.ServletRenderContext
import org.slf4j.LoggerFactory

@deprecated("Use ScalateReverseRouteSupport instead.", "2.7.0")
trait ScalateUrlGeneratorSupport extends ScalateReverseRouteSupport

trait ScalateReverseRouteSupport extends ScalateSupport {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  lazy val reflectRoutes: Map[String, Route] =
    this.getClass.getDeclaredMethods
      .filter(_.getParameterTypes.isEmpty)
      .filter(f => classOf[Route].isAssignableFrom(f.getReturnType))
      .map(f => (f.getName, f.invoke(this).asInstanceOf[Route]))
      .toMap

  private final val ReverseRoutesKey = "org.scalatra.scalate.reverseRoutes"

  protected def saveReverseRoutes(servletClassName: String, routes: Map[String, Route], sc: ServletContext): Unit = {
    val savedRoutes = savedReverseRoutes(sc)
    val servletAndRouteNames = savedRoutes.flatMap { case (servletName, routes) => routes.keys.map((servletName, _)) }
    servletAndRouteNames.foreach {
      case (otherServletName, routeName) =>
        if (routes.keys.exists(_ == routeName)) {
          logger.warn(s"Reverse route with name `${routeName}` declared in both $servletClassName and ${otherServletName} - this could cause incorrect urls to be generated!!!")
        }
    }
    sc.setAttribute(
      ReverseRoutesKey,
      savedRoutes + (servletClassName -> routes))
  }

  protected def savedReverseRoutes(sc: ServletContext): Map[String, Map[String, Route]] = {
    Option(sc.getAttribute(ReverseRoutesKey)).getOrElse(Map.empty)
      .asInstanceOf[Map[String, Map[String, Route]]]
  }

  override protected def createTemplateEngine(config: ConfigT): TemplateEngine = {
    val engine = super.createTemplateEngine(config)
    saveReverseRoutes(this.getClass.getName, reflectRoutes, this.servletContext)
    val routeBindings = this.reflectRoutes.keys map (Binding(_, classOf[Route].getName))
    engine.bindings = engine.bindings ::: routeBindings.toList
    engine
  }

  override protected def createRenderContext(out: PrintWriter)(implicit request: HttpServletRequest, response: HttpServletResponse): ServletRenderContext = {
    import scala.collection.JavaConverters._
    val context = super.createRenderContext(out).asInstanceOf[ServletRenderContext]

    for ((servletName, routes) <- savedReverseRoutes(context.servletContext)) {
      for ((name, route) <- routes) {
        val pathForCurrentRequest = request.getServletPath
        val mappingsForServletContainingRoute = servletContext.getServletRegistration(servletName).getMappings.asScala
        val pathForServletContainingRoute = mappingsForServletContainingRoute.headOption.getOrElse("")
        val pathReplacingFn = { req: HttpServletRequest =>
          route.contextPath(req)
            .replaceFirst(
              pathForCurrentRequest,
              pathForServletContainingRoute.replaceFirst("/\\*", ""))
        }
        val routeWithOwnServletPath = route.copy(contextPath = pathReplacingFn)
        context.attributes.update(name, routeWithOwnServletPath)
      }
    }
    context
  }
}
