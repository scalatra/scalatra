package org.scalatra

import javax.servlet.ServletContext

class RichServletContext(sc: ServletContext) extends AttributesMap {
  protected def attributes = sc
}