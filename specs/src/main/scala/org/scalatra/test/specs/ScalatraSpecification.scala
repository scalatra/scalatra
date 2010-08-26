package org.scalatra.test.specs

import org.scalatra.test.ScalatraTests
import org.specs._
import org.specs.specification._

trait ScalatraSpecification extends Specification with ScalatraTests {
  new SpecContext {
    beforeSpec(start())
    afterSpec(stop())
  }
}