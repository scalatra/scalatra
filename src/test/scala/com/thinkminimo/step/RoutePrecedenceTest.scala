package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class RoutePrecedenceTestBaseServlet extends Step {
  get("/override-route") {
    "base"
  }
}

class RoutePrecedenceTestChildServlet extends RoutePrecedenceTestBaseServlet {
  get("/override-route") {
    "child"
  }
}

class RoutePrecedenceTest extends StepSuite with ShouldMatchers {
  route(classOf[RoutePrecedenceTestChildServlet], "/*")

  test("Routes in child should override routes in base") {
    get("/override-route") {
      body should equal ("child")
    }
  }
}