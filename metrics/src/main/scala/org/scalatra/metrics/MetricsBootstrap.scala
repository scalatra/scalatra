package org.scalatra.metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry

trait MetricsBootstrap {
  implicit val healthCheckRegistry: HealthCheckRegistry = MetricsRegistries.healthCheckRegistry
  implicit val metricRegistry: MetricRegistry = MetricsRegistries.metricRegistry
}

object MetricsRegistries {
  val healthCheckRegistry = new HealthCheckRegistry()
  val metricRegistry = new MetricRegistry()
}