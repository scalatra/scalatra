package org.scalatra
package scalate

import java.lang.reflect.Field
import java.lang.Class
import org.fusesource.scalate.servlet.{ServletRenderContext, ServletTemplateEngine}
import org.fusesource.scalate.{TemplateEngine, Binding}

trait ScalateUrlGeneratorSupport { self: ScalateSupport =>

  lazy val reflectRoutes: Map[String, Route] =
    this.getClass.getDeclaredMethods
      .filter(_.getParameterTypes.isEmpty)
      .filter(_.getReturnType.isAssignableFrom(classOf[Route]))
      .map(f => (f.getName, f.invoke(this).asInstanceOf[Route]))
      .toMap

  override protected def configureTemplateEngine(engine: TemplateEngine) = {
    val generatorBinding = Binding("urlGenerator", classOf[UrlGeneratorSupport].getName, true)
    val routeBindings = this.reflectRoutes.keys map (Binding(_, classOf[Route].getName))
    engine.bindings = generatorBinding :: engine.bindings ::: routeBindings.toList
    engine
  }

  override protected def configureRenderContext(context: ServletRenderContext) = {
    for ((name, route) <- this.reflectRoutes)
      context.attributes.update(name, route)
    context.attributes.update("urlGenerator", UrlGenerator)
    context
  }
}
