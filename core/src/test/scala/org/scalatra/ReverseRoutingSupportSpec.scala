package org.scalatra

import org.specs2.mutable._
import org.specs2.matcher.MapMatchers._

class ReverseRoutingSupportTest extends Specification {

  val servlet = new UrlGeneratorTestServlet

  "Routes extracted from the servlet" should {
    "exist" in {
      servlet.reflectRoutes must haveValue(servlet.simpleString)
      servlet.reflectRoutes must haveValue(servlet.singleNamed)
    }
    "be indexed by their names" in {
      servlet.reflectRoutes must havePair("simpleString" -> servlet.simpleString)
      servlet.reflectRoutes must havePair("singleNamed" -> servlet.singleNamed)
    }
  }
}
