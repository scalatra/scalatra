package org.scalatra

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap

class RouteRegistry {
  private val methodRoutes: ConcurrentMap[HttpMethod, Seq[Route]] = {
    val map = new ConcurrentHashMap[HttpMethod, Seq[Route]]
    map
  }

  private var _beforeFilters: Seq[Route] = Vector.empty
  private var _afterFilters: Seq[Route] = Vector.empty

  /**
   * Returns the sequence of routes registered for the specified method.
   */
  def apply(method: HttpMethod): Seq[Route] = 
    methodRoutes.getOrElse(method, Seq.empty)

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
}
