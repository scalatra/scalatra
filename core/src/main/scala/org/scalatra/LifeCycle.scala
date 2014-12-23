package org.scalatra

import javax.servlet.ServletContext

import org.scalatra.servlet.ServletApiImplicits

trait LifeCycle extends ServletApiImplicits {
  def init(context: ServletContext) {}

  def destroy(context: ServletContext) {}
}
