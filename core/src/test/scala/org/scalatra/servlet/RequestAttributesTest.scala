package org.scalatra
package servlet

import test.scalatest.ScalatraFunSuite

class RequestAttributesTest extends ScalatraFunSuite with AttributesTest {
  addServlet(new AttributesServlet {
    def attributesMap = request
  }, "/*")
}

