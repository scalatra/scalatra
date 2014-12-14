package org.scalatra
package test
package specs2

import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }

class ScalatraSpecSpec extends ScalatraSpec {
  def is =
    s2"""
get / should
  return 'Hello, world.' $e1
"""

  // scalatra-specs2 does not depend on Scalatra, so we'll create our own
  // simple servlet for a sanity check
  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
      res.getWriter.write("Hello, world.")
    }
  }, "/*")

  def e1 = get("/") {
    body must_== "Hello, world."
  }
}
