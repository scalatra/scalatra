package org.scalatra
package scalate

import java.io.PrintWriter
import java.lang.reflect.Field
import java.lang.Class
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.fusesource.scalate.servlet.{ ServletRenderContext, ServletTemplateEngine }
import org.fusesource.scalate.{ TemplateEngine, Binding }

trait ScalateUrlGeneratorSupport extends ScalateSupport {

  lazy val reflectRoutes: Map[String, Route] =
    this.getClass.getDeclaredMethods
      .filter(_.getParameterTypes.isEmpty)
      .filter(f => classOf[Route].isAssignableFrom(f.getReturnType))
      .map(f => (f.getName, f.invoke(this).asInstanceOf[Route]))
      .toMap

  override protected def createTemplateEngine(config: ConfigT) = {
    val engine = super.createTemplateEngine(config)
    //    val generatorBinding = Binding("urlGenerator", classOf[UrlGeneratorSupport].getName, true)
    val routeBindings = this.reflectRoutes.keys map (Binding(_, classOf[Route].getName))
    engine.bindings = engine.bindings ::: routeBindings.toList
    engine
  }

  override protected def createRenderContext(req: HttpServletRequest = request, res: HttpServletResponse = response, out: PrintWriter = response.getWriter) = {
    val context = super.createRenderContext(req, res, out)
    for ((name, route) <- this.reflectRoutes)
      context.attributes.update(name, route)
    //    context.attributes.update("urlGenerator", UrlGenerator)
    context
  }
}
