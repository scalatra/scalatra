package org.scalatra.metrics

import javax.servlet.ServletContext

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import com.codahale.metrics.servlet._
import com.codahale.metrics.servlets._
import org.scalatra.servlet.ServletApiImplicits

object MetricsSupportExtensions extends ServletApiImplicits {
  class MetricsSupportExtensions(context: ServletContext)(implicit healthCheckRegistry: HealthCheckRegistry, metricRegistry: MetricRegistry) {

    def mountMetricsAdminServlet(path: String) = context.mount(classOf[AdminServlet], path)

    def mountMetricsServlet(path: String) = context.mount(classOf[MetricsServlet], path)

    def mountThreadDumpServlet(path: String) = context.mount(classOf[ThreadDumpServlet], path)

    def mountHealthCheckServlet(path: String) = context.mount(classOf[HealthCheckServlet], path)

    def installInstrumentedFilter(path: String) = context.mount(classOf[InstrumentedFilter], path)

    if (context.getAttribute("com.codahale.metrics.servlets.HealthCheckServlet.registry") == null) {
      context.setAttribute("com.codahale.metrics.servlets.HealthCheckServlet.registry", healthCheckRegistry)
    }

    if (context.getAttribute("com.codahale.metrics.servlets.MetricsServlet.registry") == null) {
      context.setAttribute("com.codahale.metrics.servlets.MetricsServlet.registry", metricRegistry)
    }

    if (context.getAttribute("com.codahale.metrics.servlet.InstrumentedFilter.registry") == null) {
      context.setAttribute("com.codahale.metrics.servlet.InstrumentedFilter.registry", metricRegistry)
    }
  }

  implicit def metricsSupportExtensions(context: ServletContext)(implicit healthCheckRegistry: HealthCheckRegistry, metricRegistry: MetricRegistry) = new MetricsSupportExtensions(context)
}