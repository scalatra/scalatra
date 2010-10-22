package org.scalatra

import java.util.NoSuchElementException
import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

object ParamsTestServlet {
  val NoSuchElement = "No Such Element"
}

class ParamsTestServlet extends ScalatraServlet {
  import ParamsTestServlet._

  get("/multiParams/:key") {
    multiParams(params("key")).mkString("[",",","]")
  }

  get("/params/:key") {
    try {
      params(params("key"))
    }
    catch {
      case _: NoSuchElementException => NoSuchElement
      case e => throw e
    }
  }

  get("/symbol/:sym") {
    params('sym)
  }

  get("/twoSymbols/:sym1/:sym2") {
    params('sym1)+" and "+ params('sym2)
  }
}

class ParamsTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[ParamsTestServlet], "/*")

  test("supports multiple parameters") {
    get("/multiParams/numbers", "numbers" -> "one", "numbers" -> "two", "numbers" -> "three") {
      body should equal ("[one,two,three]")
    }
  }

  test("unknown multiParam returns an empty seq") {
    get("/multiParams/oops") {
      status should equal (200)
      body should equal ("[]")
    }
  }

  test("params returns first value when multiple values present") {
    get("/params/numbers", "numbers" -> "one", "numbers" -> "two", "numbers" -> "three") {
      body should equal ("one")
    }
  }

  test("params on unknown key throws NoSuchElementException") {
    get ("/params/oops") {
      body should equal (ParamsTestServlet.NoSuchElement)
    }
  }

  test("can use symbols as keys for retrieval")  {
    get("/symbol/hello") {
      body should equal ("hello")
    }
  }

  test("can use symbols multiple times ")  {
    get("/twoSymbols/hello/world") {
      body should equal ("hello and world")
    }
  }
}
