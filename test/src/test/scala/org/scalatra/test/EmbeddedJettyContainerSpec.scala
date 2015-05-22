package org.scalatra.test

import javax.servlet.http._

import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll

class EmbeddedJettyContainerSpec extends SpecificationLike
    with EmbeddedJettyContainer
    with HttpComponentsClient
    with BeforeAfterAll {

  def beforeAll = start()
  def afterAll = stop()

  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) = {
      val hasDefault = getServletContext.getNamedDispatcher("default") != null
      res.addHeader("X-Has-Default-Servlet", hasDefault.toString)
      res.getWriter.print("Hello, world")
    }
  }, "/*")

  "An embedded jetty container" should {
    "respond to a hello world servlet" in {
      get("/") { body must_== "Hello, world" }
    }

    "have a default servlet" in {
      get("/") { response.header("X-Has-Default-Servlet") must_== "true" }
    }
  }
}
