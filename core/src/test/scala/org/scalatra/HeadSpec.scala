package org.scalatra

import org.scalatra.test.scalatest.ScalatraWordSpec

class HeadSpec extends ScalatraWordSpec {
  addServlet(
    new ScalatraServlet {
      get("/") {
        response.addHeader("X-Powered-By", "caffeine")
        "poof -- watch me disappear"
      }
    },
    "/*"
  )

  "A HEAD request" should {
    "return no body" in {
      head("/") {
        assert(response.body == "")
      }
    }
    "preserve headers" in {
      head("/") {
        assert(header("X-Powered-By") == "caffeine")
      }
    }
  }

}
