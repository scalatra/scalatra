package org.scalatra
package scalate

import test.specs2.ScalatraSpec

class ScalateSupportSpec extends ScalatraSpec { def is =
  "ScalateSupport should"                       ^
    "render uncaught errors with 500.scaml"     ! e1^
    "not throw a NullPointerException for trivial requests" ! e2

  addServlet(new ScalatraServlet with ScalateSupport {
    get("/barf") {
      throw new RuntimeException
    }

    get("/happy-happy") {
      "puppy dogs"
    }
  }, "/*")

  def e1 = get("/barf") {
    body must contain ("""id="scalate-error"""")
  }

  def e2 = get("/happy-happy") {
    body must_== "puppy dogs"
  }
}
