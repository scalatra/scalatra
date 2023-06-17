package org.scalatra.metrics

private[metrics] object MetricsCompat {
  object servlets {
    type AdminServlet = com.codahale.metrics.servlets.AdminServlet
    type MetricsServlet = com.codahale.metrics.servlets.MetricsServlet
    type ThreadDumpServlet = com.codahale.metrics.servlets.ThreadDumpServlet
    type HealthCheckServlet = com.codahale.metrics.servlets.HealthCheckServlet
  }

  object servlet {
    type InstrumentedFilter = com.codahale.metrics.servlet.InstrumentedFilter
  }

  val HealthCheckServletRegistryName: String = classOf[com.codahale.metrics.servlets.HealthCheckServlet].getName + ".registry"
  val MetricsServletRegistryName: String = classOf[com.codahale.metrics.servlets.MetricsServlet].getName + ".registry"
  val InstrumentedFilterRegistryName: String = classOf[com.codahale.metrics.servlet.InstrumentedFilter].getName + ".registry"
}
