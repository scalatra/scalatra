package org.scalatra

import org.scalatra.ServletCompat.ServletContext

import org.scalatra.servlet.ServletApiImplicits

trait LifeCycle extends ServletApiImplicits {

  def init(context: ServletContext): Unit = {}

  def destroy(context: ServletContext): Unit = {}

}
