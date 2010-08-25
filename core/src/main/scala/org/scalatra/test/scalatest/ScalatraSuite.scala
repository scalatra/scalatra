package org.scalatra.test
package scalatest

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ScalatraSuite extends FunSuite with ScalatraTests with BeforeAndAfterAll {
  override protected def beforeAll(): Unit = start()
  override protected def afterAll(): Unit = stop()
}