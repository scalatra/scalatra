package org.scalatra
package test
package specs2

import org.specs2.specification.{BaseSpecification, Step, Fragments}

/**
 * A base specification structure that starts the tester before the
 * specification and stops it afterward.  Clients probably want to extend
 * ScalatraSpec or MutableScalatraSpec.
 */
trait BaseScalatraSpec extends BaseSpecification with ScalatraTests {
  override def map(fs: =>Fragments) = Step(start()) ^ super.map(fs) ^ Step(stop())
}
