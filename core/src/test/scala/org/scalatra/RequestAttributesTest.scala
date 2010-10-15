package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class RequestAttributesTest extends ScalatraFunSuite with ShouldMatchers with AttributesTest {
  addServlet(new AttributesServlet {
    def attributesMap = request
  }, "/*")
}

