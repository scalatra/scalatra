package org.scalatra

import test.scalatest.ScalatraFunSuite

class ServletContextAttributesTest extends ScalatraFunSuite with AttributesTest {
  addServlet(new AttributesServlet {
    def attributesMap = servletContext
  }, "/*")
}

