package org.scalatra

import org.json4s._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.test.scalatest.ScalatraFunSuite

class JacksonJsonSupportTestServlet extends ScalatraServlet with JacksonJsonSupport {
  //implicit protected def jsonFormats: Formats = DefaultFormats
  implicit protected def jsonFormats: Formats = null

  get("/") {
    "ok"
  }
}

class JacksonJsonSupportTest extends ScalatraFunSuite {
  addServlet(classOf[JacksonJsonSupportTestServlet], "/*")

  test("works fine (issue #486)") {
    get("/") {
      status should equal(200)
      body should equal("ok")
    }
  }
}
