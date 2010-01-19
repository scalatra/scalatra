package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class FilterTestServlet extends Step {
  var beforeCount = 0
  before { beforeCount += 1 }
  get("/") { beforeCount }
}

class FilterTest extends StepSuite with ShouldMatchers {
  route(classOf[FilterTestServlet], "/*")
  
  test("before is called exactly once per request") {
    get("/") { body should equal("1") }
    get("/") { body should equal("2") }
  }
}

