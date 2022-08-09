package org.scalatra.metrics

import jakarta.servlet.{ ServletContextEvent, ServletContextListener }

import org.scalatra.metrics.MetricsSupportExtensions._
import org.scalatra.test.scalatest.ScalatraFlatSpec

class MetricsServletSupportSpec extends ScalatraFlatSpec {
  servletContextHandler.addEventListener(new ServletContextListener with MetricsBootstrap {
    override def contextDestroyed(sce: ServletContextEvent): Unit = {}

    override def contextInitialized(sce: ServletContextEvent): Unit = {
      sce.getServletContext.mountMetricsAdminServlet("/admin")
    }
  })

  "The MetricsSupportExtensions" should "Mount the admin servlet" in {
    get("/admin") {
      status should equal(200)
      body should include("Operational Menu")
    }
  }
}
