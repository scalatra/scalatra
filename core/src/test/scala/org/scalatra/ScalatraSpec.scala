package org.scalatra

import javax.servlet.http.HttpServlet
import test.ScalatraTests
import org.specs.mock.Mockito
import org.specs.Specification
import org.specs.runner.{JUnit, ScalaTest}


class ScalatraSpec  extends Specification with Mockito with JUnit with ScalaTest with ScalatraTests {
  detailedDiffs

}