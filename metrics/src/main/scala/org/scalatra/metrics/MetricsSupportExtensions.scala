package org.scalatra.metrics

import jakarta.servlet.ServletContext

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import io.dropwizard.metrics.servlet._
import io.dropwizard.metrics.servlets._
import org.scalatra.servlet.ServletApiImplicits

object MetricsSupportExtensions extends ServletApiImplicits {
  implicit class MetricsSupportExtension(context: ServletContext)(implicit healthCheckRegistry: HealthCheckRegistry, metricRegistry: MetricRegistry) {

    def mountMetricsAdminServlet(path: String) = context.mount(classOf[AdminServlet], path)

    def mountMetricsServlet(path: String) = context.mount(classOf[MetricsServlet], path)

    def mountThreadDumpServlet(path: String) = context.mount(classOf[ThreadDumpServlet], path)

    def mountHealthCheckServlet(path: String) = context.mount(classOf[HealthCheckServlet], path)

    def installInstrumentedFilter(path: String) = context.mount(classOf[InstrumentedFilter], path)

    if (context.getAttribute("io.dropwizard.metrics.servlets.HealthCheckServlet.registry") == null) {
      context.setAttribute("io.dropwizard.metrics.servlets.HealthCheckServlet.registry", healthCheckRegistry)
    }

    if (context.getAttribute("io.dropwizard.metrics.servlets.MetricsServlet.registry") == null) {
      context.setAttribute("io.dropwizard.metrics.servlets.MetricsServlet.registry", metricRegistry)
    }

    if (context.getAttribute("io.dropwizard.metrics.servlet.InstrumentedFilter.registry") == null) {
      context.setAttribute("io.dropwizard.metrics.servlet.InstrumentedFilter.registry", metricRegistry)
    }
  }
}
