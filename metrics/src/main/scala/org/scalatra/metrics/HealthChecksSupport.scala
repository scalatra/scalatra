package org.scalatra.metrics

import java.util.concurrent.Callable

import nl.grons.metrics._
import nl.grons.metrics.scala._
import org.scalatra.ScalatraBase

trait HealthChecksSupport extends nl.grons.metrics.scala.CheckedBuilder {
  self: ScalatraBase ⇒

  protected def healthChecksRegistry = HealthChecks.healthChecksRegistry

  def metricName(name: String) = MetricName(name)

  def checkHealth[A <: HealthCheckMagnet](name: String)(thunk: => A) = healthCheck(name) { thunk }
  def checkHealth[A <: HealthCheckMagnet](name: String, unhealthyMessage: String)(thunk: => A) =
    healthCheck(name, unhealthyMessage) { thunk }

  def runHealthCheck(name: String) = healthChecksRegistry.runHealthCheck(name)
  def runHealthChecks() = healthChecksRegistry.runHealthChecks()
}