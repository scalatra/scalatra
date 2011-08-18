package org.scalatra
package scalate

import test.specs2.ScalatraSpec
import org.fusesource.scalate.{TemplateSource, Binding}

class ScalateSupportSpec extends ScalatraSpec { def is =
  "ScalateSupport should"                                         ^
    "render uncaught errors with 500.scaml"                       ! e1^
    "not throw a NullPointerException for trivial requests"       ! e2^
    "render a simple template"                                    ! e3^
    "render a simple template with params"                        ! e4

  addServlet(new ScalatraServlet with ScalateSupport {

    get("/barf") {
      throw new RuntimeException
    }

    get("/happy-happy") {
      "puppy dogs"
    }

    get("/simple-template") {
      renderTemplate("/simple.jade")
    }

    get("/params") {
      renderTemplate("/params.jade", "foo" -> "Configurable")
    }

  }, "/*")

  def e1 = get("/barf") {
    body must contain ("id=\"scalate-error\"")
  }

  def e2 = get("/happy-happy") {
    body must_== "puppy dogs"
  }

  def e3 = get("/simple-template") {
    body must_== "<div>Simple template</div>\n"
  }

  def e4 = get("/params") {
    body must_== "<div>Configurable template</div>\n"
  }
}
