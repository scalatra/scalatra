package org.scalatra.twirl

import javax.servlet.http.HttpServletRequest

import org.scalatra.{ Route, UrlGenerator }
import scala.collection.JavaConverters._

trait UrlHelpers extends ReverseRouteSupport {

  /**
   * Calculate a URL for a reversible route and some params.
   *
   * @param route a reversible route
   * @param params a list of named param/value pairs
   * @return a URI that matches the route for the given params
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: String, params: (String, String)*)(implicit request: HttpServletRequest): String = {
    UrlGenerator.url(getReverseRoute(route), params: _*)(request)
  }

  /**
   * Calculate a URL for a reversible route and some splats.
   *
   * @param route a reversible route
   * @param splat the first splat parameter
   * @param moreSplats any splat parameters beyond the first
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: String, splat: String, moreSplats: String*)(implicit request: HttpServletRequest): String = {
    UrlGenerator.url(getReverseRoute(route), splat, moreSplats: _*)(request)
  }

  /**
   * Calculate a URL for a reversible route, some params, and some splats.
   *
   * @param route a reversible route
   * @param params a map of param/value pairs
   * @param splats a series of splat parameters
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: String, params: Map[String, String], splats: Iterable[String])(implicit request: HttpServletRequest): String = {
    UrlGenerator.url(getReverseRoute(route), params, splats)(request)
  }

  protected def getReverseRoute(name: String)(implicit request: HttpServletRequest): Route = {
    val sc = request.getServletContext
    val route = for {
      (servletName, routes) <- getReverseRoutes(sc)
      (routeName, route) <- routes if routeName == name
    } yield {
      val sc = request.getServletContext
      val pathForCurrentRequest = request.getServletPath
      val mappingsForServletContainingRoute = sc.getServletRegistration(servletName).getMappings.asScala
      val pathForServletContainingRoute = mappingsForServletContainingRoute.headOption.getOrElse("")
      val pathReplacingFn = { req: HttpServletRequest =>
        route.contextPath(req)
          .replaceFirst(
            pathForCurrentRequest,
            pathForServletContainingRoute.replaceFirst("/\\*", ""))
      }
      val routeWithOwnServletPath = route.copy(contextPath = pathReplacingFn)
      routeWithOwnServletPath
    }
    route.head
  }

}

/**
 * Provides helper methods to render a url by reverse routing in Twirl templates.
 *
 * {{{
 * @import org.scalatra.twirl.url._
 * @()(implicit request: javax.servlet.http.HttpServletRequest)
 * ...
 * <a href="@url("login")">Login</a>
 * ...
 * }}}
 *
 * Note: To use these helpers, your controller must extend `TwirlReverseRouteSupport`.
 */
object url extends UrlHelpers