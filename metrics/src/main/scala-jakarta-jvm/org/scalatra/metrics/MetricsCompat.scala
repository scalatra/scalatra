package org.scalatra.metrics

private[metrics] object MetricsCompat {
  object servlets {
    type AdminServlet       = io.dropwizard.metrics.servlets.AdminServlet
    type MetricsServlet     = io.dropwizard.metrics.servlets.MetricsServlet
    type ThreadDumpServlet  = io.dropwizard.metrics.servlets.ThreadDumpServlet
    type HealthCheckServlet = io.dropwizard.metrics.servlets.HealthCheckServlet
  }

  object servlet {
    type InstrumentedFilter = io.dropwizard.metrics.servlet.InstrumentedFilter
  }

  val HealthCheckServletRegistryName: String =
    classOf[io.dropwizard.metrics.servlets.HealthCheckServlet].getName + ".registry"
  val MetricsServletRegistryName: String = classOf[io.dropwizard.metrics.servlets.MetricsServlet].getName + ".registry"
  val InstrumentedFilterRegistryName: String =
    classOf[io.dropwizard.metrics.servlet.InstrumentedFilter].getName + ".registry"
}
