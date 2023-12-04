package org.scalatra

import jakarta.servlet.ServletContext

import org.scalatra.servlet.ServletApiImplicits

trait LifeCycle extends ServletApiImplicits {

  def init(context: ServletContext): Unit = {}

  def destroy(context: ServletContext): Unit = {}

}
