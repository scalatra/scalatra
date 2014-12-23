package org.scalatra.metrics

import com.codahale.metrics.health.HealthCheckRegistry
import nl.grons.metrics.scala._

trait HealthChecksSupport extends nl.grons.metrics.scala.CheckedBuilder with MetricsBootstrap {
  implicit def healthCheckRegistry: HealthCheckRegistry
  val registry = healthCheckRegistry

  def healthCheckName(name: String) = MetricName(name)

  def checkHealth(name: String)(thunk: => HealthCheckMagnet) = healthCheck(name) { thunk }
  def checkHealth(name: String, unhealthyMessage: String)(thunk: => HealthCheckMagnet) =
    healthCheck(name, unhealthyMessage) { thunk }

  def runHealthCheck(name: String) = healthCheckRegistry.runHealthCheck(name)
  def runHealthChecks() = healthCheckRegistry.runHealthChecks()
}