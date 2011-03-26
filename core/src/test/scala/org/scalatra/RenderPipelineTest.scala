package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite


class RenderPipelineTestServlet extends ScalatraPipelinedServlet {

  render[String] {
    case s @ "the string to render" => response.getWriter print ("Rendering string: %s" format s)
    case s => "Augmenting string: " + s
  }

  get("/any") {
    11111
  }

  get("/string") {
    "the string to render"
  }

  get("/augment") {
    "yet another string"
  }
}

class RenderPipelineTest  extends ScalatraFunSuite with ShouldMatchers {

  addServlet(new RenderPipelineTestServlet, "/*")

  test("should still render defaults") {
    get("/any") {
      body should equal("11111")
    }
  }

  test("should render the string") {
    get("/string") {
      body should equal("Rendering string: the string to render")
    }
  }

  test("should augment a string") {
    get("/augment") {
      body should equal("Augmenting string: yet another string")
    }
  }
}