package org.scalatra
package test
package specs2

import org.eclipse.jetty.testing.ServletTester
import org.specs2.specification.{Step, Fragments}
import org.specs2.mutable.Specification

/**
 * A Specification that starts the tester before the specification and stops it
 * afterward.
 *
 * This is a spec of the mutable variation of the specs2 framework.
 * All documentation for specs2 still applies.
 */
trait MutableScalatraSpec extends Specification with ScalatraTests {
  lazy val tester = new ServletTester

  override def map(fs: =>Fragments) = Step(start()) ^ fs ^ Step(stop())
}
