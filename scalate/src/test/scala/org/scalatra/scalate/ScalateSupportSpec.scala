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
    "render a simple template with params"                        ! e4^
    "looks for layouts in /WEB-INF/layouts"                       ! e5
    "generate a url from a template"                              ! e6

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
      templateEngine.layoutStrategy.asInstanceOf[DefaultLayoutStrategy].defaultLayouts.sortWith(_<_) mkString ";"
      templateEngine.layoutStrategy.asInstanceOf[DefaultLayoutStrategy].defaultLayouts mkString ";"
    }

    val urlGeneration = get("/url-generation") {
      renderTemplate("/urlGeneration.jade")
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
    body must_== (List(
      "/WEB-INF/layouts/default.jade",
      "/WEB-INF/layouts/default.mustache",
      "/WEB-INF/layouts/default.scaml",
      "/WEB-INF/layouts/default.ssp",
      "/WEB-INF/scalate/layouts/default.jade",
      "/WEB-INF/scalate/layouts/default.mustache",
      "/WEB-INF/scalate/layouts/default.scaml",
      "/WEB-INF/scalate/layouts/default.ssp"
    ) mkString ";")
  }

  def e6 = get("/url-generation") {
    body must_== "/url-generation\n"
  }
}
