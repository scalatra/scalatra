package org.scalatra

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap

class RouteRegistry {
  private val methodRoutes: ConcurrentMap[HttpMethod, Seq[Route]] =
    new ConcurrentHashMap[HttpMethod, Seq[Route]]

  private var _beforeFilters: Seq[Route] = Vector.empty
  private var _afterFilters: Seq[Route] = Vector.empty

  /**
   * Returns the sequence of routes registered for the specified method.
   *
   * HEAD must be identical to GET without a body, so HEAD returns GET's
   * routes.
   */
  def apply(method: HttpMethod): Seq[Route] =
    method match {
      case Head => methodRoutes.getOrElse(Get, Vector.empty)
      case m => methodRoutes.getOrElse(m, Vector.empty)
    }

  /**
   * Returns a set of methods with a matching route.
   *
   * HEAD must be identical to GET without a body, so GET implies HEAD.
   */
  def matchingMethods: Set[HttpMethod] = matchingMethodsExcept { _ => false }

  /**
   * Returns a set of methods with a matching route minus a specified
   * method.
   *
   * HEAD must be identical to GET without a body, so:
   * - GET implies HEAD
   * - filtering one filters the other
   */
  def matchingMethodsExcept(method: HttpMethod): Set[HttpMethod] = {
    val p: HttpMethod => Boolean = method match {
      case Get | Head => { m => m == Get || m == Head }
      case _ => { _ == method }
    }
    matchingMethodsExcept(p)
  }

  private def matchingMethodsExcept(p: HttpMethod => Boolean) = {
    var methods = (methodRoutes filter { case (method, routes) =>
      !p(method) && (routes exists { _().isDefined })
    }).keys.toSet
    if (methods.contains(Get))
      methods += Head
    methods
  }

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
    if (methodRoutes.putIfAbsent(method, f(Vector.empty)).isDefined) {
      val oldRoutes = methodRoutes(method)
      if (!methodRoutes.replace(method, oldRoutes, f(oldRoutes)))
       modifyRoutes(method, f)
    }
  }

  /**
   * List of entry points, made of all route matchers
   */
  def entryPoints: Iterable[String] =
    for {
      (method, routes) <- methodRoutes
      route <- routes
    } yield method + " " + route

  override def toString(): String =
    entryPoints.toSeq sortWith (_ < _) mkString ", "
}
