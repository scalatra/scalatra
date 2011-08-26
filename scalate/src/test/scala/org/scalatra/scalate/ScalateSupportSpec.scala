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
    "looks for layouts in /WEB-INF/layouts"                       ! e5^
    "generate a url from a template"                              ! e6^
    "generate a url with params from a template"                  ! e7^
    "render a simple template via jade method"                    ! e8^
    "render a simple template with params via jade method"        ! e9^
    "render a simple template via scaml method"                   ! e10^
    "render a simple template with params via scaml method"       ! e11^
    "render a simple template via ssp method"                     ! e12^
    "render a simple template with params via ssp method"         ! e13^
    "render a simple template via mustache method"                ! e14^
    "render a simple template with params via mustache method"    ! e15^
    "looks for templates in legacy /WEB-INF/scalate/templates"    ! e16^
    "looks for index page if no template found"                   ! e17

  addServlet(new ScalatraServlet with ScalateSupport with ScalateUrlGeneratorSupport {

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

    get("/jade-template") {
      jade("simple")
    }

    get("/jade-params") {
      jade("params", "foo" -> "Configurable")
    }

    get("/scaml-template") {
      scaml("simple")
    }

    get("/scaml-params") {
      scaml("params", "foo" -> "Configurable")
    }

    get("/ssp-template") {
      ssp("simple")
    }

    get("/ssp-params") {
      ssp("params", "foo" -> "Configurable")
    }

    get("/mustache-template") {
      mustache("simple")
    }

    get("/mustache-params") {
      mustache("params", "foo" -> "Configurable")
    }

    get("/layout-strategy") {
      templateEngine.layoutStrategy.asInstanceOf[DefaultLayoutStrategy].defaultLayouts.sortWith(_<_) mkString ";"
    }

    val urlGeneration = get("/url-generation") {
      renderTemplate("/urlGeneration.jade")
    }

    val urlGenerationWithParams = get("/url-generation-with-params/:a/vs/:b") {
      renderTemplate("/urlGenerationWithParams.jade", ("a" -> params("a")), ("b" -> params("b")))
    }

    get("/legacy-view-path") {
      jade("legacy")
    }

    get("/directory") {
      jade("directory/index")
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

  def e7 = get("/url-generation-with-params/jedi/vs/sith") {
    body must_== "/url-generation-with-params/jedi/vs/sith\n"
  }

  def e8 = get("/jade-template") {
    body must_== "<div>Jade template</div>\n"
  }

  def e9 = get("/jade-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e10 = get("/scaml-template") {
    body must_== "<div>Scaml template</div>\n"
  }

  def e11 = get("/scaml-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e12 = get("/ssp-template") {
    body must_== "<div>SSP template</div>\n"
  }

  def e13 = get("/ssp-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e14 = get("/mustache-template") {
    body must_== "<div>Mustache template</div>\n"
  }

  def e15 = get("/mustache-params") {
    body must_== "<div>Configurable template</div>\n"
  }

  def e16 = get("/legacy-view-path") {
    body must_== "<p>legacy</p>\n"
  }

  def e17 = get("/directory") {
    body must_== "<p>index</p>\n"
  }
}
