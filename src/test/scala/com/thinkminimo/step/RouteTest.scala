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

  get("/single-splat/*") {
    multiParams.getOrElse(":splat", Seq.empty).mkString(":")
  }

  get("/mixing-multiple-splats/*/foo/*/*") {
    multiParams.getOrElse(":splat", Seq.empty).mkString(":")
  }

  get("/mix-named-and-splat-params/:foo/*") {
    params(":foo")+":"+params(":splat")
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

  test("supports single splat params") {
    get("/single-splat/foo") {
      body should equal ("foo")
    }

    get("/single-splat/foo/bar/baz") {
      body should equal ("foo/bar/baz")
    }
  }

  test("supports mixing multiple splat params") {
    get("/mixing-multiple-splats/bar/foo/bling/baz/boom") {
      body should equal ("bar:bling:baz/boom")
    }
  }

  test("supports mixing named and splat params") {
    get("/mix-named-and-splat-params/foo/bar/baz") {
      body should equal ("foo:bar/baz")
    }
  }

}