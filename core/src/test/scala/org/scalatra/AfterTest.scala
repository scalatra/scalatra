package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import test.scalatest.ScalatraFunSuite
import javax.servlet.ServletConfig


class AfterTestServlet extends ScalatraServlet with AfterTestAppBase
class AfterTestApp(config: ServletConfig, req: HttpServletRequest, res: HttpServletResponse) extends ScalatraApp(config, req, res) with AfterTestAppBase
trait AfterTestAppBase extends ScalatraSyntax {

  after() {
    response.setStatus(204)
  }

  after("/some/path") {
    response.setStatus(202)
  }

  after("/other/path") {
    response.setStatus(206)
  }

  get("/some/path") { }

  get("/other/path") { }

  get("/third/path") { }

}

class AfterServletTest extends AfterTest {
  mount(classOf[AfterTestServlet], "/*")
}
class AfterAppTest extends AfterTest {
  mount(new AfterTestApp(_, _, _), "/*")
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
