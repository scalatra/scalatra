package org.scalatra

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap

class RouteRegistry {
  private val routeMap: ConcurrentMap[HttpMethod, Seq[Route]] = {
    val map = new ConcurrentHashMap[HttpMethod, Seq[Route]]
    map
  }

  /**
   * Returns the sequence of routes registered for the specified method.
   */
  def apply(method: HttpMethod): Seq[Route] = 
    routeMap.getOrElse(method, Seq.empty)

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

  @tailrec private def modifyRoutes(method: HttpMethod, f: (Seq[Route] => Seq[Route])): Unit = {
    if (routeMap.putIfAbsent(method, f(Vector.empty)).isDefined) {
      val oldRoutes = routeMap(method)
      if (!routeMap.replace(method, oldRoutes, f(oldRoutes)))
       modifyRoutes(method, f)
    }
  }
}
