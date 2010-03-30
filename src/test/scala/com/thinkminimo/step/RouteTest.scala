/* 
 * Most of these test cases are ported from http://github.com/sinatra/sinatra/tree master/test/routing_test.rb
 */
package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class RouteTestServlet extends Step {
  get(params.getOrElse("booleanTest", "false") == "true") {
    "matched boolean route"    
  }

  get("/optional/?:foo?/?:bar?") {
    (for (key <- List(":foo", ":bar") if params.isDefinedAt(key)) yield key+"="+params(key)).mkString(";")
  }
}

class RouteTest extends StepSuite with ShouldMatchers {
  route(classOf[RouteTestServlet], "/*")

  test("routes can be a boolean expression") {
    get("/whatever", "booleanTest" -> "true") {
      body should equal ("matched boolean route")
    }
  }

  test("supports optional named params") {
    get("/optional/hello/world") {
      body should equal (":foo=hello;:bar=world")
    }

    get("/optional/hello") {
      body should equal (":foo=hello")
    }

    get("/optional") {
      body should equal ("")
    }
  }
}