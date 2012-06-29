package org.scalatra

import javax.servlet.ServletContext
import servlet.ServletApplicationContext

trait LifeCycle {
  def init(context: ServletContext) {}

  def destroy(context: ServletContext) {}

  protected implicit def enrichServletContext(servletContext: ServletContext) =
    new ServletApplicationContext(servletContext)
}
