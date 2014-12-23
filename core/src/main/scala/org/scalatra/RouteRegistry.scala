package org.scalatra

import java.util.concurrent.ConcurrentHashMap

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.concurrent.{ Map => ConcurrentMap }

class RouteRegistry {

  private[this] val _methodRoutes: ConcurrentMap[HttpMethod, Seq[Route]] =
    new ConcurrentHashMap[HttpMethod, Seq[Route]].asScala

  private[this] val _statusRoutes: ConcurrentMap[Int, Route] =
    new ConcurrentHashMap[Int, Route].asScala

  private[this] var _beforeFilters: Seq[Route] = Vector.empty
  private[this] var _afterFilters: Seq[Route] = Vector.empty

  /**
   * Returns the sequence of routes registered for the specified method.
   *
   * HEAD must be identical to GET without a body, so HEAD returns GET's
   * routes.
   */
  def apply(method: HttpMethod): Seq[Route] =
    method match {
      case Head => _methodRoutes.getOrElse(Head, _methodRoutes.getOrElse(Get, Vector.empty))
      case m => _methodRoutes.getOrElse(m, Vector.empty)
    }

  /**
   * Return a route for a specific HTTP response status code.
   * @param statusCode the status code.
   *
   */
  def apply(statusCode: Int): Option[Route] = _statusRoutes.get(statusCode)

  /**
   * Returns a set of methods with a matching route.
   *
   * HEAD must be identical to GET without a body, so GET implies HEAD.
   */
  def matchingMethods(requestPath: String): Set[HttpMethod] = matchingMethodsExcept(requestPath) { _ => false }

  /**
   * Returns a set of methods with a matching route minus a specified
   * method.
   *
   * HEAD must be identical to GET without a body, so:
   * - GET implies HEAD
   * - filtering one filters the other
   */
  def matchingMethodsExcept(method: HttpMethod, requestPath: String): Set[HttpMethod] = {
    val p: HttpMethod => Boolean = method match {
      case Get | Head => { m => m == Get || m == Head }
      case _ => { _ == method }
    }
    matchingMethodsExcept(requestPath)(p)
  }

  private def matchingMethodsExcept(requestPath: String)(p: HttpMethod => Boolean) = {
    var methods = (_methodRoutes filter { kv =>
      val method = kv._1
      val routes = kv._2
      !p(method) && (routes exists (_.apply(requestPath).isDefined))
    }).keys.toSet
    if (methods.contains(Get))
      methods += Head
    methods
  }

  /**
   * Add a route that explicitly matches one or more response codes.
   */
  def addStatusRoute(codes: Range, route: Route) = codes.foreach { code => _statusRoutes.put(code, route) }

  /**
   * Prepends a route to the method's route sequence.
   */
  def prependRoute(method: HttpMethod, route: Route): Unit =
    modifyRoutes(method, route +: _)

  /**
   * Removes a route from the method's route seqeuence.
   */
  def removeRoute(method: HttpMethod, route: Route): Unit =
    modifyRoutes(method, _ filterNot (_ == route))

  /**
   * Returns the sequence of filters to run before the route.
   */
  def beforeFilters: Seq[Route] = _beforeFilters

  /**
   * Appends a filter to the sequence of before filters.
   */
  def appendBeforeFilter(route: Route): Unit = _beforeFilters :+= route

  /**
   * Returns the sequence of filters to run after the route.
   */
  def afterFilters: Seq[Route] = _afterFilters

  /**
   * Appends a filter to the sequence of before filters.
   */
  def appendAfterFilter(route: Route): Unit = _afterFilters :+= route

  @tailrec private def modifyRoutes(method: HttpMethod, f: (Seq[Route] => Seq[Route])): Unit = {
    if (_methodRoutes.putIfAbsent(method, f(Vector.empty)).isDefined) {
      val oldRoutes = _methodRoutes(method)
      if (!_methodRoutes.replace(method, oldRoutes, f(oldRoutes)))
        modifyRoutes(method, f)
    }
  }

  /**
   * List of entry points, made of all route matchers
   */
  def entryPoints: Seq[String] =
    (for {
      (method, routes) <- _methodRoutes
      route <- routes
    } yield method + " " + route).toSeq sortWith (_ < _)

  def methodRoutes: Map[HttpMethod, Seq[Route]] = _methodRoutes.clone().toMap

  override def toString: String = entryPoints mkString ", "

}
