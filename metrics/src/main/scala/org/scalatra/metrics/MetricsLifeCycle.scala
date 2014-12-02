package org.scalatra.metrics

import com.codahale.metrics.servlets._
import org.scalatra.LifeCycle

trait MetricsLifeCycle {
  self: LifeCycle ⇒

  def metricsAdminServlet = new AdminServlet
  def metricsServlet = new MetricsServlet
  def healthCheckServlet = new HealthCheckServlet
  def threadDumpServlet = new ThreadDumpServlet
  def pingServlet = new PingServlet
}
