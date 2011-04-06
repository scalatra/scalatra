package org.scalatra.specs2

import org.scalatra.test.ScalatraTests
import org.specs2._
import org.eclipse.jetty.testing.ServletTester
import specification.{Step, Fragments}

/**
 * A Specification that starts the tester before the specification and stops it afterward.
 *
 * This is a spec of the immutable variation of the specs2 framework.
 * All documentation for specs2 still applies (except for the fact that you don't have to close a spec with the end fragment).
 */
trait ScalatraSpec extends Specification with ScalatraTests { self: Specification =>

  lazy val tester = new ServletTester
  
  def startStep = Step(start())
  def stopStep = Step(stop())

  abstract override def is = {
    startStep     ^
    super.is      ^
    stopStep      ^ end
  }
}