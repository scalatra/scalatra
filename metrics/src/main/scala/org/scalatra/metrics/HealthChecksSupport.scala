package org.scalatra.metrics

import com.codahale.metrics.health.HealthCheckRegistry
import nl.grons.metrics.scala._

trait HealthChecksSupport extends nl.grons.metrics.scala.CheckedBuilder with MetricsBootstrap {
  implicit def healthCheckRegistry: HealthCheckRegistry
  val registry = healthCheckRegistry

  private type ToMagnet[T] = ByName[T] => HealthCheckMagnet

  def healthCheckName(name: String) = MetricName(name)

  def checkHealth[T](name: String)(checker: => T)(implicit toMagnet: ToMagnet[T]) =
    healthCheck(name) { checker }
  def checkHealth[T](name: String, unhealthyMessage: String)(checker: => T)(implicit toMagnet: ToMagnet[T]) =
    healthCheck(name, unhealthyMessage) { checker }

  def runHealthCheck(name: String) = healthCheckRegistry.runHealthCheck(name)
  def runHealthChecks() = healthCheckRegistry.runHealthChecks()
}
