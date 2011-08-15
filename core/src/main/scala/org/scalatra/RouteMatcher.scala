package org.scalatra

class RouteMatcher(
  val matcher: () => Option[ScalatraKernel.MultiParams],
  val pattern: String
) {

  def apply(): Option[ScalatraKernel.MultiParams] = matcher()

  override def toString: String = pattern
}
