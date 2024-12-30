package org.scalatra.metrics

import org.scalatra.ServletCompat.ServletContext

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import org.scalatra.metrics.MetricsCompat.HealthCheckServletRegistryName
import org.scalatra.metrics.MetricsCompat.MetricsServletRegistryName
import org.scalatra.metrics.MetricsCompat.InstrumentedFilterRegistryName
import org.scalatra.metrics.MetricsCompat.servlet._
import org.scalatra.metrics.MetricsCompat.servlets._
import org.scalatra.servlet.ServletApiImplicits

object MetricsSupportExtensions extends ServletApiImplicits {
  implicit class MetricsSupportExtension(context: ServletContext)(implicit
      healthCheckRegistry: HealthCheckRegistry,
      metricRegistry: MetricRegistry
  ) {

    def mountMetricsAdminServlet(path: String) = context.mount(classOf[AdminServlet], path)

    def mountMetricsServlet(path: String) = context.mount(classOf[MetricsServlet], path)

    def mountThreadDumpServlet(path: String) = context.mount(classOf[ThreadDumpServlet], path)

    def mountHealthCheckServlet(path: String) = context.mount(classOf[HealthCheckServlet], path)

    def installInstrumentedFilter(path: String) = context.mount(classOf[InstrumentedFilter], path)

    if (context.getAttribute(HealthCheckServletRegistryName) == null) {
      context.setAttribute(HealthCheckServletRegistryName, healthCheckRegistry)
    }

    if (context.getAttribute(MetricsServletRegistryName) == null) {
      context.setAttribute(MetricsServletRegistryName, metricRegistry)
    }

    if (context.getAttribute(InstrumentedFilterRegistryName) == null) {
      context.setAttribute(InstrumentedFilterRegistryName, metricRegistry)
    }
  }
}
