package org.scalatra.servlet

import org.scalatra.ServletCompat.ServletContext

trait MountConfig {
  def apply(ctxt: ServletContext): Unit
}
