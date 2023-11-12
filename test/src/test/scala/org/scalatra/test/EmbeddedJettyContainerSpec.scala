package org.scalatra.test

import org.scalatra.ServletCompat.http._

import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll

class EmbeddedJettyContainerSpec extends SpecificationLike
  with EmbeddedJettyContainer
  with HttpComponentsClient
  with BeforeAfterAll {

  def beforeAll(): Unit = start()
  def afterAll(): Unit = stop()

  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) = {
      req.getRequestURI() match {
        case "/" =>
          val hasDefault = getServletContext.getNamedDispatcher("default") != null
          res.addHeader("X-Has-Default-Servlet", hasDefault.toString)
          res.getWriter.print("Hello, world")
        case "/json" =>
          res.setContentType("application/json")
          res.getWriter.print("{message: '日本語'}")
        case "/charset" =>
          res.setContentType("text/plain; charset=UTF-8")
          res.getWriter.print("日本語")
        case _ =>
          res.setStatus(404)
      }
    }
  }, "/*")

  "An embedded jetty container" should {
    "respond to a hello world servlet" in {
      get("/") { body must_== "Hello, world" }
    }

    "have a default servlet" in {
      get("/") { response.header("X-Has-Default-Servlet") must_== "true" }
    }

    "handle charset of application/json" in {
      get("/json") {
        body must_== "{message: '日本語'}"
      }
    }

    "handle charset parameter in Content-Type header" in {
      get("/charset") {
        body must_== "日本語"
      }
    }
  }
}
