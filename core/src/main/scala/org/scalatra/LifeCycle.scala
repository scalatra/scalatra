package org.scalatra

trait LifeCycle {
  def init(context: ApplicationContext): Unit = {}

  def destroy(context: ApplicationContext): Unit = {}
}
