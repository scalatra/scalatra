package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite

class UrlGeneratorContextTestServlet extends ScalatraServlet with UrlGeneratorSupport {
  val servletRoute: Route = get("/foo") { url(servletRoute) }
}

class UrlGeneratorContextTestFilter extends ScalatraFilter {
  val filterRoute: Route = get("/filtered/foo") {
    UrlGenerator.url(filterRoute)
  }
}

class UrlGeneratorSupportTest extends ScalatraFunSuite {
  addServlet(new UrlGeneratorContextTestServlet, "/*")
  addServlet(new UrlGeneratorContextTestServlet, "/servlet-path/*")
  addServlet(new UrlGeneratorContextTestServlet, "/filtered/*")
  addFilter(new UrlGeneratorContextTestFilter, "/*")

  test("Url of a servlet mounted on /*") {
    get("/foo") {
      body should equal("/foo")
    }
  }

  test("Url of a servlet mounted on /servlet-path/*") {
    get("/servlet-path/foo") {
      body should equal("/servlet-path/foo")
    }
  }

  test("Url of a filter does not duplicate the servlet path") {
    get("/filtered/foo") {
      body should equal("/filtered/foo")
    }
  }
}

class UrlGeneratorNonRootContextSupportTest extends ScalatraFunSuite {
  override def contextPath = "/context"

  addServlet(new UrlGeneratorContextTestServlet, "/*")
  addServlet(new UrlGeneratorContextTestServlet, "/servlet-path/*")
  addServlet(new UrlGeneratorContextTestServlet, "/filtered/*")
  addFilter(new UrlGeneratorContextTestFilter, "/*")

  test("Url of a servlet mounted on /*") {
    get("/context/foo") {
      body should equal("/context/foo")
    }
  }

  test("Url of a servlet mounted on /servlet-path/*") {
    get("/context/servlet-path/foo") {
      body should equal("/context/servlet-path/foo")
    }
  }

  test("Url of a filter does not duplicate the servlet path") {
    get("/context/filtered/foo") {
      body should equal("/context/filtered/foo")
    }
  }
}

