package org.scalatra.servlet

import jakarta.servlet.ServletContext

trait MountConfig {
  def apply(ctxt: ServletContext): Unit
}
