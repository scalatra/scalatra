package org.scalatra
package atmosphere

import servlet.ScalatraListener
import javax.servlet.ServletContextEvent
import _root_.akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object ScalatraAtmosphereListener {
  val ActorSystemKey = "org.scalatra.atmosphere.ActorSystem"
}
class ScalatraAtmosphereListener extends ScalatraListener {
  import ScalatraAtmosphereListener._
  override def contextInitialized(sce: ServletContextEvent) {
    configureServletContext(sce)
    configureAkkaSystem(sce)
    configureCycleClass()
  }

  private[this] def configureAkkaSystem(sce: ServletContextEvent) {
    val ctxt = sce.getServletContext
    val system = ActorSystem("scalatra", ConfigFactory.load.getConfig("scalatra"))
    ctxt.setAttribute(ActorSystemKey, system)
  }
}