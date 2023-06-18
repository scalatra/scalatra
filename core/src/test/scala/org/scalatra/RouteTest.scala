/*
 * Most of these test cases are ported from https://github.com/sinatra/sinatra/tree master/test/routing_test.rb
 */
package org.scalatra

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraFunSuite

class RouteTestServlet extends ScalatraServlet {
  get("/foo") {
    "matched simple string route"
  }

  get(params.getOrElse("booleanTest", "false") == "true") {
    "matched boolean route"
  }

  get("/optional/?:foo?/?:bar?") {
    (for (key <- List("foo", "bar") if params.isDefinedAt(key)) yield key + "=" + params(key)).mkString(";")
  }

  get("/optional-ext.?:ext?") {
    (for (key <- List("ext") if params.isDefinedAt(key)) yield key + "=" + params(key)).mkString(";")
  }

  get("/single-splat/*") {
    multiParams.getOrElse("splat", Seq.empty).mkString(":")
  }

  get("/mixing-multiple-splats/*/foo/*/*") {
    multiParams.getOrElse("splat", Seq.empty).mkString(":")
  }

  get("/mix-named-and-splat-params/:foo/*") {
    params("foo") + ":" + params("splat")
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

  get("/fail", false, new RouteMatcher {
    def apply(requestPath: String) = { throw new RuntimeException("shouldn't execute"); None }
  }) {
    "shouldn't return"
  }

  get("/encoded-uri/:name") {
    params("name")
  }

  get("/encoded-uri/:name1/:name2") {
    s"'${params("name1")}' & '${params("name2")}'"
  }

  get("/encoded-uri-2/中国话不用彁字。") {
    "中国话不用彁字。"
  }

  get("/encoded-uri-3/%C3%B6") {
    "ö"
  }

  get("/semicolon/?") {
    "semicolon"
  }

  get("/semicolon/document") {
    "document"
  }
}

class RouteTestNotDecodePathServlet extends ScalatraServlet {
  decodePercentEncodedPath = false

  get("/encoded-uri/:name") {
    params("name")
  }

  get("/encoded-uri/:name1/:name2") {
    s"'${params("name1")}' & '${params("name2")}'"
  }
}

@RunWith(classOf[JUnitRunner])
class RouteTest extends ScalatraFunSuite {
  addServlet(classOf[RouteTestServlet], "/*")

  addServlet(new ScalatraServlet {
    get("/") { "root" }
  }, "/subcontext/*")

  test("routes can be a simple string") {
    get("/foo") {
      body should equal("matched simple string route")
    }
  }

  test("routes can be a boolean expression") {
    get("/whatever", "booleanTest" -> "true") {
      body should equal("matched boolean route")
    }
  }

  test("supports optional named params") {
    get("/optional/hello/world") {
      body should equal("foo=hello;bar=world")
    }

    get("/optional/hello") {
      body should equal("foo=hello")
    }

    get("/optional") {
      body should equal("")
    }

    get("/optional-ext.json") {
      body should equal("ext=json")
    }

    get("/optional-ext") {
      body should equal("")
    }
  }

  test("supports single splat params") {
    get("/single-splat/foo") {
      body should equal("foo")
    }

    get("/single-splat/foo/bar/baz") {
      body should equal("foo/bar/baz")
    }
  }

  test("supports mixing multiple splat params") {
    get("/mixing-multiple-splats/bar/foo/bling/baz/boom") {
      body should equal("bar:bling:baz/boom")
    }
  }

  test("supports mixing named and splat params") {
    get("/mix-named-and-splat-params/foo/bar/baz") {
      body should equal("foo:bar/baz")
    }
  }

  test("matches a dot ('.') as part of a named param") {
    get("/dot-in-named-param/user@example.com/name") {
      body should equal("user@example.com")
    }
  }

  test("matches a literal dot ('.') outside of named params") {
    get("/dot-outside-named-param/pony.jpg") {
      header("file") should equal("pony")
      header("ext") should equal("jpg")
    }
  }

  test("literally matches . in paths") {
    get("/literal.dot.in.path") {
      body should equal("matched literal dot")
    }
  }

  test("literally matches $ in paths") {
    get("/test$") {
      body should equal("test$")
    }
  }

  test("literally matches + in paths") {
    get("/te+st") {
      body should equal("te+st")
    }
  }

  test("literally matches () in paths") {
    get("/test(bar)") {
      body should equal("test(bar)")
    }
  }

  test("literally matches ; in paths") {
    get("/foo;123") {
      status should equal(200)
    }
  }

  test("supports conditional path routes") {
    get("/conditional", "condition" -> "true") {
      body should equal("true")
    }

    get("/conditional") {
      body should equal("false")
    }
  }

  test("supports regular expressions") {
    get("/foooom/bar") {
      body should equal("regex match")
    }
  }

  test("makes regular expression captures available in params(\"captures\")") {
    get("/foorooomma/baf") {
      body should equal("orooomma:f")
    }
  }

  test("supports conditional regex routes") {
    get("/regular-expression", "condition" -> "true") {
      body should equal("regex: true")
    }

    get("/regular-expression", "condition" -> "false") {
      body should equal("regex: false")
    }
  }

  test("a route with no matchers matches all requests to that method") {
    post("/an-arbitrary-path") {
      body should equal("I match any post!")
    }
  }

  test("matchers should not execute if one before it fails") {
    get("/fail") {
      body should not include ("shouldn't return")
    }
  }

  test("trailing slash is optional in a subcontext-mapped servlet") {
    get("/subcontext") {
      body should equal("root")
    }

    get("/subcontext/") {
      body should equal("root")
    }
  }

  test("handles encoded characters in uri") {

    get("/encoded-uri/ac/dc") {
      status should equal(200)
      body should equal("'ac' & 'dc'")
    }

    // '%2F' cannot be distinguished from '/'
    get("/encoded-uri/ac%2Fdc") {
      status should equal(200)
      body should equal("'ac' & 'dc'")
    }

    get("/encoded-uri/%23toc") {
      status should equal(200)
      body should equal("#toc")
    }

    get("/encoded-uri/%3Fquery") {
      status should equal(200)
      body should equal("?query")
    }

    get("/encoded-uri/Fu%C3%9Fg%C3%A4nger%C3%BCberg%C3%A4nge%3F%23") {
      status should equal(200)
      body should equal("Fußgängerübergänge?#")
    }

    get("/encoded-uri/ö%C3%B6%25C3%25B6") {
      // With Apache HTTP Client 5.2.1, this invalid URL cannot be sent
      pending
      status should equal(200)
      body should equal("öö%C3%B6")
    }

    get("/encoded-uri-2/中国话不用彁字。") {
      status should equal(200)
    }

    get("/encoded-uri-2/%E4%B8%AD%E5%9B%BD%E8%AF%9D%E4%B8%8D%E7%94%A8%E5%BD%81%E5%AD%97%E3%80%82") {
      status should equal(200)
    }

    // mixing encoded with decoded characters
    get("/encoded-uri-2/中国%E8%AF%9D%E4%B8%8D%E7%94%A8%E5%BD%81%E5%AD%97%E3%80%82") {
      status should equal(200)
    }

    get("/encoded-uri-3/%25C3%25B6") {
      status should equal(200)
    }
  }

  test("should chop off uri starting from semicolon") {
    get("/semicolon;jessionid=9328475932475") {
      status should equal(200)
      body should equal("semicolon")
    }

    get("/semicolon/;jessionid=9328475932475") {
      status should equal(200)
      body should equal("semicolon")
    }

    get("/semicolon/document;version=3/section/2") {
      status should equal(200)
      body should equal("document")
    }
  }
}

class RouteNotDecodePathTest extends ScalatraFunSuite {
  addServlet(classOf[RouteTestNotDecodePathServlet], "/*")

  test("handles encoded characters in uri but don't decode") {
    get("/encoded-uri/ac/dc") {
      status should equal(200)
      body should equal("'ac' & 'dc'")
    }

    // '%2F' is distinguished from '/'
    get("/encoded-uri/ac%2Fdc") {
      status should equal(200)
      body should equal("ac%2Fdc")
    }

    get("/encoded-uri/ö%C3%B6%25C3%25B6") {
      // With Apache HTTP Client 5.2.1, this invalid URL cannot be sent
      pending
      status should equal(200)
      body should equal("%C3%B6%C3%B6%25C3%25B6")
    }

    get("/encoded-uri/Fu%C3%9Fg%C3%A4nger%C3%BCberg%C3%A4nge%3F%23") {
      status should equal(200)
      body should equal("Fu%C3%9Fg%C3%A4nger%C3%BCberg%C3%A4nge%3F%23")
    }
  }
}
