package org.scalatra

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import test.scalatest.ScalatraFunSuite
import javax.servlet.ServletConfig

class AfterTestServlet extends ScalatraServlet with AfterTestAppBase
trait AfterTestAppBase extends ScalatraBase {

  after() {
    response.setStatus(204)
  }

  after("/some/path") {
    response.setStatus(202)
  }

  after("/other/path") {
    response.setStatus(206)
  }

  get("/some/path") {}

  get("/other/path") {}

  get("/third/path") {}

}

class AfterServletTest extends AfterTest {
  mount(classOf[AfterTestServlet], "/*")
}
abstract class AfterTest extends ScalatraFunSuite {

  test("afterAll is applied to all paths") {
    get("/third/path") {
      status should equal(204)
    }
  }

  test("after only applies to a given path") {
    get("/some/path") {
      status should equal(202)
    }
    get("/other/path") {
      status should equal(206)
    }
  }

}
