package org.scalatra
package test
package specs2

import org.specs2.Specification

/**
 * A Specification that starts the tester before the specification and stops it
 * afterward.
 *
 * This is a spec of the immutable variation of the specs2 framework.
 * All documentation for specs2 still applies.
 */
trait ScalatraSpec extends Specification with BaseScalatraSpec
