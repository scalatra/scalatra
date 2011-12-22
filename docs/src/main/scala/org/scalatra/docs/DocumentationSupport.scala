package org.scalatra.docs

import org.scalatra._

trait DocumentationSupport extends ScalatraKernel {

  implicit def documentation2RouteMatcher(documentation: Documentation): RouteMatcher =
    new SinatraRouteMatcher(documentation.route, requestPath)

  def doc(documentation: Documentation): RouteTransformer = { route =>
    route.copy(metadata = route.metadata + (docsSymbol -> documentation))
  }

  def docs(): Seq[Documentation] = {
    (for{
      (method, routes) <- routes.methodRoutes
      route <- routes
      docAsAny <- route.metadata.get(docsSymbol) if docAsAny.isInstanceOf[Documentation]
      doc <- Some(docAsAny.asInstanceOf[Documentation])
    } yield doc.copy(method = method)).toSeq
  }

  def docsAsHtml(): String = {
    docs map ( _.toHtml ) mkString ("<ul><li>", "</li><li>", "</li></ul>")
  }

}