package org.scalatra.test
package scalatest

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.eclipse.jetty.testing.ServletTester
import org.scalatest.{Suite, BeforeAndAfterAll}

@RunWith(classOf[JUnitRunner])
trait ScalatraSuite extends ScalatraTests with BeforeAndAfterAll {
  this: Suite =>

  lazy val tester = new ServletTester

  override protected def beforeAll(): Unit = start()
  override protected def afterAll(): Unit = stop()
}