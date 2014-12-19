package org.scalatra

import org.scalatra.test.scalatest._

class UrlSupportTest extends ScalatraFunSuite {
  override def contextPath = "/context"

  addServlet(new ScalatraServlet {
    get("/") {
      if (params.contains("session")) session // trigger a jsessionid
      this.url(params("url"), params - "url", absolutize = false)
    }

    get("/option") {
      this.url(params("url"), Seq("id" -> params.get("id")), absolutize = false)
    }

    get("/strip-context") {
      this.url(params("url")) //, includeContextPath = false)
    }
  }, "/*")

  def url(url: String, params: Map[String, String] = Map.empty) =
    get("/context/", params + ("url" -> url)) { response.body }

  test("a page-relative URL should not have the context path prepended") {
    url("page-relative") should equal("page-relative")
  }

  test("a should expand an option") {
    url("page-relative", Map("id" -> "the-id")) should equal("page-relative?id=the-id")
  }

  test("a context-relative URL should have the context path prepended") {
    url("/context-relative") should equal("/context/context-relative")
  }

  test("an absolute URL should not have the context path prepended") {
    url("http://www.example.org/") should equal("http://www.example.org/")
  }

  test("empty params should not generate a query string") {
    url("foo", Map.empty) should equal("foo")
  }

  test("a '/' should come out as /context") {
    get("/context/strip-context?url=/") { body should equal("/context") }
  }

  test("a '' should come out as /") {
    get("/context/strip-context?url=") { body should equal("") }
  }

  test("params should be rendered as a query string") {
    val params = Map("one" -> "uno", "two" -> "dos")
    val result = url("en-to-es", params)
    val Array(path, query) = result.split("""\?""")
    val urlParams = query.split("&")
    urlParams.toSet should equal(Set("one=uno", "two=dos"))
  }

  test("params should url encode both keys and values in UTF-8") {
    url("de-to-ru", Map("fünf" -> "пять")) should equal("de-to-ru?f%C3%BCnf=%D0%BF%D1%8F%D1%82%D1%8C")
  }

  test("encodes URL through response") {
    session {
      url("foo", Map("session" -> "session")) should include("jsessionid=")
    }
  }
}
