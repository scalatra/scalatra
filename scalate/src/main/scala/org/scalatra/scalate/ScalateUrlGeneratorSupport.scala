package org.scalatra
package scalate

import java.io.PrintWriter
import org.fusesource.scalate.{TemplateEngine, Binding}

trait ScalateUrlGeneratorSupport extends ScalateSupport {

  lazy val reflectRoutes: Map[String, Route] =
    this.getClass.getDeclaredMethods
      .filter(_.getParameterTypes.isEmpty)
      .filter(f => classOf[Route].isAssignableFrom(f.getReturnType))
      .map(f => (f.getName, f.invoke(this).asInstanceOf[Route]))
      .toMap

  override protected def createTemplateEngine(config: AppContext) = {
    val engine = super.createTemplateEngine(config)
    val generatorBinding = Binding.of[UrlGeneratorSupport]("urlGenerator", importMembers = true)
    val routeBindings = this.reflectRoutes.keys map (Binding(_, classOf[Route].getName))
    engine.bindings = generatorBinding :: engine.bindings ::: routeBindings.toList
    engine
  }

  override protected def createRenderContext(req: HttpRequest = request, res: HttpResponse = response, out: PrintWriter = response.writer) = {
    val context = super.createRenderContext(req, res, out)
    for ((name, route) <- this.reflectRoutes)
      context.attributes(name) = route
    context.attributes("urlGenerator") = UrlGenerator
    context
  }
}
