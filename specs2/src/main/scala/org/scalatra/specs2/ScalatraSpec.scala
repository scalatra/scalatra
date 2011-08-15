package org.scalatra.specs2

import org.scalatra.test.ScalatraTests
import org.eclipse.jetty.testing.ServletTester
import org.specs2.execute.Result
import org.specs2.specification.{Around, BeforeAfter, Step, Fragments}
import org.specs2.Specification

/**
 * A Specification that starts the tester before the specification and stops it afterward.
 *
 * This is a spec of the immutable variation of the specs2 framework.
 * All documentation for specs2 still applies.
 */
trait ScalatraSpec extends Specification with ScalatraTests {
  lazy val tester = new ServletTester

  override def map(fs: =>Fragments) = Step(start()) ^ fs ^ Step(stop())
}
