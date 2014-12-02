package org.scalatra.metrics

import org.scalatra.ScalatraBase

trait MetricsSupport {
  self: ScalatraBase ⇒

  protected def metricsRegistry = Metrics.metricRegistry

  def timer(name: String) = metricsRegistry.timer(name)
}