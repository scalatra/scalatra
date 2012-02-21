package org.scalatra

import javax.servlet.ServletContext

trait LifeCycle extends ServletApiImplicits {
  def init(servletContext: ServletContext): Unit = {}

  def destroy(servletContext: ServletContext): Unit = {}
}
