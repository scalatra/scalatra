package org.scalatra

trait LifeCycle {
  def init(context: AppContext) {}

  def destroy(context: AppContext) {}
}
