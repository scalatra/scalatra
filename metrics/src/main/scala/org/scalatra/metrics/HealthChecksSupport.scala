package org.scalatra.metrics

import nl.grons.metrics4.scala._

trait HealthChecksSupport extends CheckedBuilder with MetricsBootstrap {
  val registry = healthCheckRegistry

  private type ToMagnet[T] = ByName[T] => HealthCheckMagnet

  def healthCheckName(name: String) = MetricName(name)

  def checkHealth[T](name: String)(checker: => T)(implicit
      toMagnet: ToMagnet[T]
  ) =
    healthCheck(name)(checker)
  def checkHealth[T](name: String, unhealthyMessage: String)(checker: => T)(
      implicit toMagnet: ToMagnet[T]
  ) =
    healthCheck(name, unhealthyMessage)(checker)

  def runHealthCheck(name: String) = healthCheckRegistry.runHealthCheck(name)
  def runHealthChecks() = healthCheckRegistry.runHealthChecks()
}
