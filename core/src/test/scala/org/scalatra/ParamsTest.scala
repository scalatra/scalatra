package org.scalatra

import java.util.NoSuchElementException
import test.scalatest.ScalatraFunSuite
import io.Codec

object ParamsTestServlet {
  val NoSuchElement = "No Such Element"
}

class ParamsTestServlet extends ScalatraServlet {
  import ParamsTestServlet._

  get("/multiParams/:key") {
    multiParams(params("key")).mkString("[", ",", "]")
  }

  get("/multiParams/:key") {
    multiParams(params("key")).mkString("[", ",", "]")
  }

  get("/params/:key") {
    try {
      params(params("key"))
    } catch {
      case _: NoSuchElementException => NoSuchElement
    }
  }

  get("/symbol/:sym") {
    params('sym)
  }

  get("/twoSymbols/:sym1/:sym2") {
    params('sym1) + " and " + params('sym2)
  }

  post("/read-body") {
    "body: " + request.body
  }
}

class ParamsTest extends ScalatraFunSuite {
  addServlet(classOf[ParamsTestServlet], "/*")

  test("supports multiple parameters") {
    get("/multiParams/numbers", "numbers" -> "one", "numbers" -> "two", "numbers" -> "three") {
      body should equal("[one,two,three]")
    }
  }

  test("supports multiple parameters with ruby like syntax") {
    get("/multiParams/numbers_ruby", "numbers_ruby[]" -> "one", "numbers_ruby[]" -> "two", "numbers_ruby[]" -> "three") {
      body should equal("[one,two,three]")
    }
  }

  test("unknown multiParam returns an empty seq") {
    get("/multiParams/oops") {
      status should equal(200)
      body should equal("[]")
    }
  }

  test("params returns first value when multiple values present") {
    get("/params/numbers", "numbers" -> "one", "numbers" -> "two", "numbers" -> "three") {
      body should equal("one")
    }
  }

  test("params on unknown key throws NoSuchElementException") {
    get("/params/oops") {
      body should equal(ParamsTestServlet.NoSuchElement)
    }
  }

  test("can use symbols as keys for retrieval") {
    get("/symbol/hello") {
      body should equal("hello")
    }
  }

  test("can use symbols multiple times ") {
    get("/twoSymbols/hello/world") {
      body should equal("hello and world")
    }
  }

  test("can read the body of a post") {
    post("/read-body", "hi".getBytes(Codec.UTF8.charSet)) {
      body should equal("body: hi")
    }
  }
}
