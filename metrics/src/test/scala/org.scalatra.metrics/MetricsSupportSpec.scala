package org.scalatra.metrics

import org.scalatra.ScalatraServlet
import org.scalatra.test.scalatest.ScalatraFlatSpec

class MetricsSupportSpec extends ScalatraFlatSpec {

  class TestServlet extends ScalatraServlet with MetricsSupport {
    get("/") {
      timer("test") {

      }

      counter("testCounter") += 1
    }
  }

  addServlet(new TestServlet, "/")

  "The Metrics support" should "not error out using metrics" in {
    get("/") {
      status should equal(200)
    }
  }
}