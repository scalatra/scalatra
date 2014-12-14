package org.scalatra
package scalate

import org.specs2.mutable._
import org.specs2.matcher.MapMatchers._

class ScalateUrlGeneratorSupportTest extends Specification {

  val servlet = new ScalatraServlet with ScalateSupport with ScalateUrlGeneratorSupport {

    val cat: String = "meea"

    val simpleString = get("/foo") {}

    val singleNamed = get("/foo/:bar") {}
  }

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
