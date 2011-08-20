package org.scalatra

import java.lang.reflect.Field
import java.lang.Class

/**
 * Mixin to ScalatraKernel that allows to extract the routes.
 */
trait ReverseRoutingSupport { self: ScalatraKernel =>

  lazy val reflectRoutes: Map[String, Route] =
    this.getClass.getDeclaredMethods
      .filter(_.getParameterTypes.isEmpty)
      .filter(_.getReturnType.isAssignableFrom(classOf[Route]))
      .map(f => (f.getName, f.invoke(this).asInstanceOf[Route]))
      .toMap
}
