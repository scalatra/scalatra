package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class ServletContextAttributesTest extends ScalatraFunSuite with ShouldMatchers with AttributesTest {
  addServlet(new AttributesServlet {
    def attributesMap = servletContext
  }, "/*")
}

