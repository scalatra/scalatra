package org.scalatra

import javax.servlet.ServletContext


package object atmosphere {
  type AtmoReceive = PartialFunction[InboundMessage, Unit]

  implicit def servletContext2AtmosphereContext(ctxt: ServletContext): AtmosphereServletContext =
      new AtmosphereServletContext(ctxt)

}
