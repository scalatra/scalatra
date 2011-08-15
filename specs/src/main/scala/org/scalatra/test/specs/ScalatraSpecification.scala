package org.scalatra.test.specs

import org.scalatra.test.ScalatraTests
import org.specs._
import scala.util.DynamicVariable
import org.eclipse.jetty.testing.ServletTester

/**
 * A Specification that starts the tester before the specification and stops it afterward.
 */
trait ScalatraSpecification extends Specification with ScalatraTests {
  // This hack lets us share the tester through the specification, starting/stopping it only once, while resetting
  // the rest of the fixture variables for each example according to the default execution model of specs.
  private lazy val _tester = new DynamicVariable[ServletTester](new ServletTester)
  def tester = _tester.value

  doBeforeSpec { start() }
  doAfterSpec { stop() }

  shareVariables()
}
