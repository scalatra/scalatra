package org.scalatra.metrics

import jakarta.servlet.ServletContext
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import io.dropwizard.metrics.servlet.InstrumentedFilter
import io.dropwizard.metrics.servlets.{ AdminServlet, HealthCheckServlet, MetricsServlet, ThreadDumpServlet }
import org.scalatra.servlet.ServletApiImplicits

object MetricsSupportExtensions extends ServletApiImplicits {
  implicit class MetricsSupportExtension(context: ServletContext)(implicit healthCheckRegistry: HealthCheckRegistry, metricRegistry: MetricRegistry) {

    private val HealthCheckServletRegistryName: String = classOf[io.dropwizard.metrics.servlets.HealthCheckServlet].getName + ".registry"
    private val MetricsServletRegistryName: String = classOf[io.dropwizard.metrics.servlets.MetricsServlet].getName + ".registry"
    private val InstrumentedFilterRegistryName: String = classOf[io.dropwizard.metrics.servlet.InstrumentedFilter].getName + ".registry"

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
