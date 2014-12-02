package org.scalatra.metrics

import com.codahale.metrics.health.HealthCheckRegistry

object HealthChecks {
  val healthChecksRegistry = new HealthCheckRegistry()
}
