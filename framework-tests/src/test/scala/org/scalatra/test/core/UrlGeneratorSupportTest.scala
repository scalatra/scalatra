package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class UrlGeneratorContextTestServlet extends ScalatraApp {
  val servletRoute: Route = get("/foo") { UrlGenerator.url(servletRoute) }
}
/*
class UrlGeneratorContextTestFilter extends ScalatraFilter {
  val filterRoute: Route = get("/filtered/foo") {
    UrlGenerator.url(filterRoute)
  }
}*/

abstract class UrlGeneratorSupportTest extends ScalatraFunSuite {
  mount(new UrlGeneratorContextTestServlet)
  mount("/servlet-path", new UrlGeneratorContextTestServlet)
//  addServlet(new UrlGeneratorContextTestServlet, "/servlet-path/*")
//  addServlet(new UrlGeneratorContextTestServlet, "/filtered/*")
//  addFilter(new UrlGeneratorContextTestFilter, "/*")

  test("Url of a servlet mounted on /*") {
    get("/foo") {
      body should equal ("/foo")
    }
  }

  test("Url of a servlet mounted on /servlet-path/*") {
    get("/servlet-path/foo") {
      body should equal ("/servlet-path/foo")
    }
  }
//
//  TODO: Do we still need this??
//  test("Url of a filter does not duplicate the servlet path") {
//    get("/filtered/foo") {
//      body should equal ("/filtered/foo")
//    }
//  }
}

abstract class UrlGeneratorNonRootContextSupportTest extends ScalatraFunSuite {
  mount("/context", new UrlGeneratorContextTestServlet)
  mount("/context/servlet-path", new UrlGeneratorContextTestServlet)


  test("Url of a servlet mounted on /*") {
    get("/context/foo") {
      body should equal ("/context/foo")
    }
  }

  test("Url of a servlet mounted on /servlet-path/*") {
    get("/context/servlet-path/foo") {
      body should equal ("/context/servlet-path/foo")
    }
  }
//
//  TODO: Do we still need this??
//  test("Url of a filter does not duplicate the servlet path") {
//    get("/context/filtered/foo") {
//      body should equal ("/context/filtered/foo")
//    }
//  }
}

class NettyUrlGeneratorSupportTest extends UrlGeneratorSupportTest with NettyBackend
class NettyUrlGeneratorNonRootContextSupportTest extends UrlGeneratorNonRootContextSupportTest with NettyBackend

class JettyUrlGeneratorSupportTest extends UrlGeneratorSupportTest with JettyBackend
class JettyUrlGeneratorNonRootContextSupportTest extends UrlGeneratorNonRootContextSupportTest with JettyBackend
