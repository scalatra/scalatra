package org.scalatra
package scalate

import specs2.ScalatraSpec

class ScalateSupportSpec extends ScalatraSpec { def is =
  "ScalateSupport should"                       ^
    "render uncaught errors with 500.scaml"     ! e1

  addServlet(new ScalatraServlet with ScalateSupport {
    get("/barf") {
      throw new RuntimeException
    }
  }, "/*")

  def e1 = get("/barf") {
    body must contain ("""id="scalate-error"""")
  }
}
