package org.scalatra.docs

import org.scalatra._

@deprecated("See scalatra-swagger", "2.1.0")
trait DocumentationSupport {
  this: ScalatraBase =>

  def name(value: String): RouteTransformer = { route =>
    route.copy(metadata = route.metadata + (docsNameSymbol -> value))
  }

  def description(value: String): RouteTransformer = { route =>
    route.copy(metadata = route.metadata + (docsDescriptionSymbol -> value))
  }

  def document(value: Boolean): RouteTransformer = { route =>
    route.copy(metadata = route.metadata + (docsDocumentSymbol -> value))
  }

  def optionalParams(value: List[Param]): RouteTransformer = { route =>
    route.copy(metadata = route.metadata + (docsOptionalParams -> value))
  }

  def requiredParams(value: List[Param]): RouteTransformer = { route =>
    route.copy(metadata = route.metadata + (docsRequiredParams -> value))
  }

  def docs(): Seq[Documentation] = {
    (for{
      (method, routes) <- routes.methodRoutes
      route <- routes
    } yield Documentation(route, method)).toSeq
  }

  def allRoutesAsHtml(): String =
    docs map ( _.toHtml ) mkString ("<ul><li>", "</li><li>", "</li></ul>")

  def allDocumentedRoutesAsHtml(): String =
    docs filter ( _.document ) map ( _.toHtml ) mkString ("<ul><li>", "</li><li>", "</li></ul>") toString

}
