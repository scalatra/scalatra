package org.scalatra
package test
package specs2

import org.specs2.Specification
import org.specs2.specification._
import org.specs2.main.ArgumentsShortcuts._

import SeleniumWebBrowser._

/**
 * A base specification structure that starts the tester before the
 * specification and stops it afterward.  Clients probably want to extend
 * ScalatraSpec or MutableScalatraSpec.
 */
trait BaseSeleniumSpec extends BaseSpecification with SeleniumTests {
  override def map(fs: =>Fragments) = sequential ^ Step(start()) ^ super.map(fs) ^ Step(stop())
}

trait MutableSeleniumSpec extends Specification with BaseSeleniumSpec

trait SeleniumSpec extends Specification with BaseSeleniumSpec