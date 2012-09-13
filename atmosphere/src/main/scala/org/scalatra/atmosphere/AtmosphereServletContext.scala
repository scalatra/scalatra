package org.scalatra
package atmosphere

import javax.servlet.ServletContext
import org.atmosphere.cpr.{ApplicationConfig, AtmosphereServlet}

class AtmosphereServletContext(context: ServletContext) {

  def enableAtmosphere(useNative: Boolean = true, useBlocking: Boolean = false) { // TODO: Add all the relevant config options
    val atmoServlet = classOf[AtmosphereServlet]
    val reg = context.addServlet(atmoServlet.getSimpleName, atmoServlet)
    reg.setInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, useNative.toString)
    reg.setInitParameter(ApplicationConfig.PROPERTY_BLOCKING_COMETSUPPORT, useBlocking.toString)
    reg.addMapping("/*")
  }


}