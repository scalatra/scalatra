package org.scalatra.servlet

import javax.servlet.ServletContext

trait MountConfig {
  def apply(ctxt: ServletContext)
}
