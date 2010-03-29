package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class RouteTestServlet extends Step {
  get(params.getOrElse("booleanTest", "false") == "true") {
    "matched boolean route"    
  }
}

class RouteTest extends StepSuite with ShouldMatchers {
  route(classOf[RouteTestServlet], "/*")

  test("routes can be a boolean expression") {
    get("/whatever", "booleanTest" -> "true") {
      body should equal ("matched boolean route")
    }
  }
}