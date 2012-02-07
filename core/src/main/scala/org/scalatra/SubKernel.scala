package org.scalatra

abstract class SubKernel(parent: CoreDsl with ScalatraKernel.Routing, path: String) extends CoreDsl with ScalatraKernel.Routing {
  implicit def servletContext = parent.servletContext

  implicit def request = parent.request

  def params = parent.params

  def multiParams = parent.multiParams

  implicit def response = parent.response

  def notFound(block: => Any) { throw new UnsupportedOperationException("Not found can only be defined on top level apps")}

  def methodNotAllowed(block: (Set[HttpMethod]) => Any) {
    throw new UnsupportedOperationException("method not allowed can only be defined on top level apps")
  }

  def error(handler: ErrorHandler) {
    throw new UnsupportedOperationException("An error handler can only be defined on top level apps")
  }

  def requestPath = parent.requestPath + path

  protected[scalatra] def routeBasePath = parent.routeBasePath + path


  protected[scalatra] def addRoute(method: HttpMethod, transformers: Seq[RouteTransformer], action: => Any): Route = {
    val route = Route(transformers, () => action, () => routeBasePath)
    parent.routes.prependRoute(method, route)
    route
  }

  protected[scalatra] def appendBeforeFilter(route: Route) { parent.appendBeforeFilter(route) }

  protected[scalatra] def appendAfterFilter(route: Route) { parent.appendAfterFilter(route) }

  protected[scalatra] def removeRoute(method: HttpMethod, route: Route) { parent.removeRoute(method, route)}
}
