package org.scalatra.metrics

import org.scalatra.ScalatraServlet
import org.scalatra.test.scalatest.ScalatraFlatSpec

class HealthCheckSupportSpec extends ScalatraFlatSpec {

  class TestServlet extends ScalatraServlet with HealthChecksSupport {
    get("/") {
      val test = checkHealth("database") { true }
      test.execute()
    }
  }

  addServlet(new TestServlet, "/")

  "The HealthCheck support" should "not error out using a health check" in {
    get("/") {
      status should equal (200)
    }
  }
}