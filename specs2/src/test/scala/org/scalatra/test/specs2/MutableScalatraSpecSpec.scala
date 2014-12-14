package org.scalatra
package test
package specs2

import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }

class MutableScalatraSpecSpec extends MutableScalatraSpec {
  // scalatra-specs2 does not depend on Scalatra, so we'll create our own
  // simple servlet for a sanity check
  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
      res.getWriter.write("Hello, world.");
    }
  }, "/*")

  "get" should {
    "be able to verify the response body" in {
      get("/") {
        body must_== "Hello, world."
      }
    }
  }
}
