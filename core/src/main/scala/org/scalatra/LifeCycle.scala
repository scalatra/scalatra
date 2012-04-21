package org.scalatra

trait LifeCycle {
  def init(context: ApplicationContext) {}

  def destroy(context: ApplicationContext) {}
}
