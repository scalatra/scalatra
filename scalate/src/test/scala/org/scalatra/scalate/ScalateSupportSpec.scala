package org.scalatra
package scalate

import test.specs2.ScalatraSpec
import org.fusesource.scalate.{TemplateSource, Binding}
import org.fusesource.scalate.layout.DefaultLayoutStrategy

class ScalateSupportSpec extends ScalatraSpec { def is =
  "ScalateSupport should"                                         ^
    "render uncaught errors with 500.scaml"                       ! e1^
    "not throw a NullPointerException for trivial requests"       ! e2^
    "render a simple template"                                    ! e3^
    "render a simple template with params"                        ! e4
    "looks for layouts in /WEB-INF/layouts"                       ! e5

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

    get("/layout-strategy") {
      templateEngine.layoutStrategy.asInstanceOf[DefaultLayoutStrategy].defaultLayouts mkString ";"
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

  // Testing the default layouts is going to be hard, but we can at least
  // verify that it's looking in the right place.
  def e5 = get("/layout-strategy") {
    body.split(";") must_== Array(
      "/WEB-INF/layout/default.jade",
      "/WEB-INF/layout/default.mustache",
      "/WEB-INF/layout/default.scaml",
      "/WEB-INF/layout/default.ssp",
      "/WEB-INF/scalate/layout/default.jade",
      "/WEB-INF/scalate/layout/default.mustache",
      "/WEB-INF/scalate/layout/default.scaml",
      "/WEB-INF/scalate/layout/default.ssp"
    )
  }
}
