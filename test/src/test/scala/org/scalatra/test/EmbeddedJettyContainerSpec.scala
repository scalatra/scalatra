package org.scalatra.test

import javax.servlet.http._
import dispatch._
import org.specs2.mutable._
import org.specs2.specification.{Step, Fragments}

class EmbeddedJettyContainerSpec extends Specification
  with EmbeddedJettyContainer
  with DispatchClient
{
  override def map(fs: =>Fragments) =
    Step(start()) ^ super.map(fs) ^ Step(stop())

  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) = {
      res.getWriter.print("Hello, world")
    }
  }, "/*")

  "An embedded jetty container" should {
    "respond to a hello world servlet" in {
      get("/") { body must_== "Hello, world" }
    }
  }
}
