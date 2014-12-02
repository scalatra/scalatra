package org.scalatra.metrics

import com.codahale.metrics.MetricRegistry

object Metrics {
  val metricRegistry = new MetricRegistry()
}
