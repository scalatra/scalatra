/*
 * Most of these test cases are ported from http://github.com/sinatra/sinatra/tree master/test/routing_test.rb
 */
package org.scalatra

import test.scalatest.ScalatraFunSuite

class RouteTestServlet extends ScalatraServlet {
  get("/foo") {
    "matched simple string route"
  }

  get(params.getOrElse("booleanTest", "false") == "true") {
    "matched boolean route"
  }

  get("/optional/?:foo?/?:bar?") {
    (for (key <- List("foo", "bar") if params.isDefinedAt(key)) yield key+"="+params(key)).mkString(";")
  }

  get("/single-splat/*") {
    multiParams.getOrElse("splat", Seq.empty).mkString(":")
  }

  get("/mixing-multiple-splats/*/foo/*/*") {
    multiParams.getOrElse("splat", Seq.empty).mkString(":")
  }

  get("/mix-named-and-splat-params/:foo/*") {
    params("foo")+":"+params("splat")
  }

  get("/dot-in-named-param/:foo/:bar") {
    params("foo")
  }

  get("/dot-outside-named-param/:file.:ext") {
    List("file", "ext") foreach { x => response.setHeader(x, params(x)) }
  }

  get("/literal.dot.in.path") {
    "matched literal dot"
  }

  get("/test$") {
    "test$"
  }

  get("/te+st") {
    "te+st"
  }

  get("/test(bar)") {
    "test(bar)"
  }

  get("/conditional") {
    "false"
  }

  get("/conditional", params.getOrElse("condition", "false") == "true") {
    "true"
  }

  get("""^\/fo(.*)/ba(.*)""".r) {
    multiParams.getOrElse("captures", Seq.empty) mkString (":")
  }

  get("""^/foo.../bar$""".r) {
    "regex match"
  }

  get("""/reg(ular)?-ex(pression)?""".r) {
    "regex: false"
  }

  get("""/reg(ular)?-ex(pression)?""".r, params.getOrElse("condition", "false") == "true") {
    "regex: true"
  }

  post() {
    "I match any post!"
  }

  get("/nothing", false) {
    "shouldn't return"
  }

  get("/fail", false, new RouteMatcher(
    () => { throw new RuntimeException("shouldn't execute"); None },
    "None matcher"
  )) {
    "shouldn't return"
  }
}

class RouteTest extends ScalatraFunSuite {
  addServlet(classOf[RouteTestServlet], "/*")

  test("routes can be a simple string") {
    get("/foo") {
      body should equal ("matched simple string route")
    }
  }

  test("routes can be a boolean expression") {
    get("/whatever", "booleanTest" -> "true") {
      body should equal ("matched boolean route")
    }
  }

  test("supports optional named params") {
    get("/optional/hello/world") {
      body should equal ("foo=hello;bar=world")
    }

    get("/optional/hello") {
      body should equal ("foo=hello")
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

  test("matches a dot ('.') as part of a named param") {
    get("/dot-in-named-param/user@example.com/name") {
      body should equal("user@example.com")
    }
  }

  test("matches a literal dot ('.') outside of named params") {
    get("/dot-outside-named-param/pony.jpg") {
      header("file") should equal ("pony")
      header("ext") should equal ("jpg")
    }
  }

  test("literally matches . in paths") {
    get("/literal.dot.in.path") {
      body should equal("matched literal dot")
    }
  }

  test("literally matches $ in paths") {
    get("/test$") {
      body should equal ("test$")
    }
  }

  test("literally matches + in paths") {
    get("/te+st") {
      body should equal ("te+st")
    }
  }

  test("literally matches () in paths") {
    get("/test(bar)") {
      body should equal ("test(bar)")
    }
  }

  test("supports conditional path routes") {
    get("/conditional", "condition" -> "true") {
      body should equal ("true")
    }

    get("/conditional") {
      body should equal ("false")
    }
  }

  test("supports regular expressions") {
    get("/foooom/bar") {
      body should equal ("regex match")
    }
  }

  test("makes regular expression captures available in params(\"captures\")") {
    get("/foorooomma/baf") {
      body should equal ("orooomma:f")
    }
  }

  test("supports conditional regex routes") {
    get("/regular-expression", "condition" -> "true") {
      body should equal ("regex: true")
    }

    get("/regular-expression", "condition" -> "false") {
      body should equal ("regex: false")
    }
  }

  test("a route with no matchers matches all requests to that method") {
    post("/an-arbitrary-path") {
      body should equal ("I match any post!")
    }
  }

  test("matchers should not execute if one before it fails") {
    get("/fail") {
      body should not include ("shouldn't return")
    }
  }
}
