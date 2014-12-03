package org.scalatra.metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry

trait MetricsBootstrap {
  implicit val healthCheckRegistry: HealthCheckRegistry = new HealthCheckRegistry()
  implicit val metricRegistry: MetricRegistry = new MetricRegistry()
}
