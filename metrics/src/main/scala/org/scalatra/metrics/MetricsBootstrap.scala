package org.scalatra.metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry

trait MetricsBootstrap {
  implicit def healthCheckRegistry: HealthCheckRegistry = MetricsRegistries.healthCheckRegistry
  implicit def metricRegistry: MetricRegistry = MetricsRegistries.metricRegistry
}

object MetricsRegistries {
  val healthCheckRegistry = new HealthCheckRegistry()
  val metricRegistry = new MetricRegistry()
}
