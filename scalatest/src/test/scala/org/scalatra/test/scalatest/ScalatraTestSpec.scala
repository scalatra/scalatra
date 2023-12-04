package org.scalatra
package test
package scalatest

import jakarta.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }

class ScalatraTestSpec extends ScalatraFunSuite {

  test("get test") {
    get("/") {
      assertResult(200) {
        status
      }

      assertResult("Hello, world.") {
        body
      }
    }
  }

  // scalatra-scalatest does not depend on Scalatra, so we'll create our own
  // simple servlet for a sanity check
  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = {
      res.getWriter.write("Hello, world.")
    }
  }, "/*")

}
