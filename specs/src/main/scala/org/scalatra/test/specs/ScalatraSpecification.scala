package org.scalatra.test.specs

import org.scalatra.test.ScalatraTests
import org.specs._

/**
 * A Specification that starts the tester before the specification and stops it afterward.
 */
@deprecated("Upgrade to Specs2.", "2.2.0")
trait ScalatraSpecification extends Specification with ScalatraTests {
  doBeforeSpec { start() }
  doAfterSpec { stop() }

  shareVariables()
}
