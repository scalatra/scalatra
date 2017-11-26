package org.scalatra.twirl

import javax.servlet.ServletContext

import org.scalatra.{ Route, ScalatraBase }
import org.slf4j.LoggerFactory

trait ReverseRouteSupport {

  private[this] val logger = LoggerFactory.getLogger(getClass)
  private val ReverseRoutesKey = "org.scalatra.twirl.reverseRoutes"

  def getReverseRoutes(sc: ServletContext): Map[String, Map[String, Route]] = {
    Option(sc.getAttribute(ReverseRoutesKey)).getOrElse(Map.empty)
      .asInstanceOf[Map[String, Map[String, Route]]]
  }

  def saveReverseRoutes(servletClassName: String, routes: Map[String, Route], sc: ServletContext): Unit = {
    val savedRoutes = getReverseRoutes(sc)
    val servletAndRouteNames = savedRoutes.flatMap { case (servletName, routes) => routes.keys.map((servletName, _)) }
    servletAndRouteNames.foreach {
      case (otherServletName, routeName) =>
        if (routes.keys.exists(_ == routeName)) {
          logger.warn(s"Reverse route with name `${routeName}` declared in both $servletClassName and ${otherServletName} - this could cause incorrect urls to be generated!!!")
        }
    }
    sc.setAttribute(
      ReverseRoutesKey,
      savedRoutes + (servletClassName -> routes))
  }

}

trait TwirlReverseRouteSupport extends ScalatraBase with ReverseRouteSupport {

  lazy val reflectRoutes: Map[String, Route] =
    this.getClass.getDeclaredMethods
      .filter(_.getParameterTypes.isEmpty)
      .filter(f => classOf[Route].isAssignableFrom(f.getReturnType))
      .map(f => (f.getName, f.invoke(this).asInstanceOf[Route]))
      .toMap

  override def initialize(config: ConfigT): Unit = {
    super.initialize(config)
    saveReverseRoutes(this.getClass.getName, reflectRoutes, this.servletContext)
  }

}
