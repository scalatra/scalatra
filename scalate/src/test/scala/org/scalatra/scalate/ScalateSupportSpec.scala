package org.scalatra
package scalate

import test.specs2.ScalatraSpec
import org.fusesource.scalate.{TemplateSource, Binding}
import org.fusesource.scalate.layout.DefaultLayoutStrategy

class ScalateSupportSpec extends ScalatraSpec { def is =
  "ScalateSupport should"                                         ^
    "render uncaught errors with 500.scaml"                       ! e1^ br ^
    "not throw a NullPointerException for trivial requests"       ! e2^br ^
    "render a simple template"                                    ! e3^br ^
    "render a simple template with params"                        ! e4^br ^
    "looks for layouts in /WEB-INF/layouts"                       ! e5^br ^
    "generate a url from a template"                              ! e6^br ^
    "generate a url with params from a template"                  ! e7^br ^
    "render a simple template via jade method"                    ! e8^br ^
    "render a simple template with params via jade method"        ! e9^br ^
    "render a simple template via scaml method"                   ! e10^br ^
    "render a simple template with params via scaml method"       ! e11^br ^
    "render a simple template via ssp method"                     ! e12^br ^
    "render a simple template with params via ssp method"         ! e13^br ^
    "render a simple template via mustache method"                ! e14^br ^
    "render a simple template with params via mustache method"    ! e15^br ^
    "looks for templates in legacy /WEB-INF/scalate/templates"    ! e16^br ^
    "looks for index page if no template found"                   ! e17^br ^
    "implicitly bind flash"                                       ! e18^br ^
    "implicitly bind session"                                     ! e19^br ^
    "implicitly bind params"                                      ! e20^br ^
    "implicitly bind multiParams"                                 ! e21^br ^
    "set templateAttributes when creating a render context"       ! e22^br ^
    "render to a string instead of response"                      ! e23^br ^
    "set status to 500 when rendering 500.scaml"                  ! e24^br ^
    end



  addServlet(new ScalatraServlet with ScalateSupport
    with ScalateUrlGeneratorSupport with FlashMapSupport {

    get("/barf") {
      throw new RuntimeException
    }

    get("/happy-happy") {
      "puppy dogs"
    }

    get("/simple-template") {
      layoutTemplate("/simple.jade")
    }

    get("/params") {
      layoutTemplate("/params.jade", "foo" -> "Configurable")
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
      templateEngine.layoutStrategy.asInstanceOf[DefaultLayoutStrategy].defaultLayouts mkString ";"
    }

    val urlGeneration = get("/url-generation") {
      layoutTemplate("/urlGeneration.jade")
    }

    val urlGenerationWithParams = get("/url-generation-with-params/:a/vs/:b") {
      layoutTemplate("/urlGenerationWithParams.jade", ("a" -> params("a")), ("b" -> params("b")))
    }

    get("/legacy-view-path") {
      jade("legacy")
    }

    get("/directory") {
      jade("directory/index")
    }

    get("/bindings/*") {
      flash.now("message") = "flash works"
      session("message") = "session works"
      jade(requestPath)
    }

    get("/bindings/params/:foo") {
      jade("/bindings/params")
    }

    get("/bindings/multiParams/*/*") {
      jade("/bindings/multiParams")
    }

    get("/template-attributes") {
      templateAttributes("foo") = "from attributes"
      scaml("params")
    }

    get("/render-to-string") {
      response.setHeader("X-Template-Output", layoutTemplate("simple"))
    }
  }, "/*")

  def e1 = get("/barf") {
    body must contain ("id=\"scalate-error\"")
  }

  def e2 = get("/happy-happy") {
    body must_== "puppy dogs"
  }

  def e3 = get("/simple-template") {
    body must_== "<div>Jade template</div>\n"
  }

  def e4 = get("/params") {
    body must_== "<div>Configurable template</div>\n"
  }

  // Testing the default layouts is going to be hard, but we can at least
  // verify that it's looking in the right place.
  def e5 = get("/layout-strategy") {
    body must_== (List(
      "/WEB-INF/templates/layouts/default.mustache",
      "/WEB-INF/templates/layouts/default.ssp",
      "/WEB-INF/templates/layouts/default.scaml",
      "/WEB-INF/templates/layouts/default.jade",
      "/WEB-INF/layouts/default.mustache",
      "/WEB-INF/layouts/default.ssp",
      "/WEB-INF/layouts/default.scaml",
      "/WEB-INF/layouts/default.jade",
      "/WEB-INF/scalate/layouts/default.mustache",
      "/WEB-INF/scalate/layouts/default.ssp",
      "/WEB-INF/scalate/layouts/default.scaml",
      "/WEB-INF/scalate/layouts/default.jade"
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
    body must_== "<div>SSP template</div>"
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

  def e18 = get("/bindings/flash") {
    body must_== "<div>flash works</div>\n"
  }

  def e19 = get("/bindings/session") {
    body must_== "<div>session works</div>\n"
  }

  def e20 = get("/bindings/params/bar") {
    body must_== "<div>bar</div>\n"
  }

  def e21 = get("/bindings/multiParams/bar/baz") {
    body must_== "<div>bar;baz</div>\n"
  }

  def e22 = get("/template-attributes") {
    body must_== "<div>from attributes template</div>\n"
  }

  def e23 = get("/render-to-string") {
    val hdr = header("X-Template-Output")
    hdr must_== "<div>SSP template</div>"
  }

  def e24 = get("/barf") {
    status must_== 500
  }
}
