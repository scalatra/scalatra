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

  get("/hide-route") {
    "hidden by later route"
  }

  get("/hide-route") {
    "visible"
  }
}

class RoutePrecedenceTest extends StepSuite with ShouldMatchers {
  route(classOf[RoutePrecedenceTestChildServlet], "/*")

  test("Routes in child should override routes in base") {
    get("/override-route") {
      body should equal ("child")
    }
  }

  test("Routes declared later in the same class take precedence") {
    /*
     * This is the opposite of Sinatra, where the earlier route wins.  But to do otherwise, while also letting child
     * classes override base classes' routes, proves to be difficult in an internal Scala DSL.  Sorry, Sinatra users.
     */
    get("/hide-route") {
      "visible"
    }
  }
}