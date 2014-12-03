package org.scalatra.metrics

import javax.servlet.{ServletContextEvent, ServletContextListener}

import org.scalatra.metrics.MetricsSupportExtensions._
import org.scalatra.test.scalatest.ScalatraFlatSpec

class MetricsServletSupportSpec extends ScalatraFlatSpec {
  servletContextHandler.addEventListener(new ServletContextListener with MetricsBootstrap {
    def contextDestroyed(sce: ServletContextEvent): Unit = {}

    def contextInitialized(sce: ServletContextEvent): Unit = {
        sce.getServletContext.mountMetricsAdminServlet("/admin")
    }
  })

  "The MetricsSupportExtensions" should "Mount the admin servlet" in {
    get("/admin") {
      status should equal (200)
      body should include ("Operational Menu")
    }
  }
}