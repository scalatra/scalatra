package org.scalatra.metrics

import com.codahale.metrics.servlets._
import org.scalatra.LifeCycle

trait MetricsLifeCycle {
  self: LifeCycle ⇒

  lazy val metricsAdminServlet = new AdminServlet
  lazy val metricsServlet = new MetricsServlet
  lazy val healthCheckServlet = new HealthCheckServlet
  lazy val threadDumpServlet = new ThreadDumpServlet
  lazy val pingServlet = new PingServlet
}
