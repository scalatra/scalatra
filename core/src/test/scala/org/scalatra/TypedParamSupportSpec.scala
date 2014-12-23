package org.scalatra

import org.scalatra.test.specs2._

class MyScalatraServlet extends ScalatraServlet {

  get("/render/:aNumber") {
    val intValue: Option[Int] = params.getAs[Int]("aNumber")

    <p>Value is { intValue getOrElse (-1) }</p>
  }
}

class TypedParamSupportSpec extends MutableScalatraSpec {

  addServlet(classOf[MyScalatraServlet], "/*")

  "GET /render/ with a Int param" should {
    "render it if the param is effectively an Int" in {
      get("/render/1000") {
        status must_== 200
        body must_== "<p>Value is 1000</p>"
      }
    }
    "render -1 if the implicit conversion fails" in {
      get("/render/foo") {
        status must_== 200
        body must_== "<p>Value is -1</p>"
      }
    }
  }
}