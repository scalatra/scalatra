package org.scalatra.test
package scalatest

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.{ JUnitSuite, JUnit3Suite, JUnitRunner }
import org.scalatest.testng.TestNGSuite

@RunWith(classOf[JUnitRunner]) /**
 * Provides Scalatra test support to ScalaTest suites.  The servlet tester
 * is started before the first test in the suite and stopped after the last.
 */
trait ScalatraSuite extends Suite with ScalatraTests with BeforeAndAfterAll with Matchers {
  override protected def beforeAll(): Unit = start()
  override protected def afterAll(): Unit = stop()
}

/**
 * Convenience trait to add Scalatra test support to JUnit3Suite.
 */
trait ScalatraJUnit3Suite extends JUnit3Suite with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to JUnitSuite.
 */
trait ScalatraJUnitSuite extends JUnitSuite with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to TestNGSuite.
 */
trait ScalatraTestNGSuite extends TestNGSuite with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to FeatureSpec.
 */
trait ScalatraFeatureSpec extends FeatureSpec with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to Spec.
 */
trait ScalatraSpec extends FunSpec with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to FlatSpec.
 */
trait ScalatraFlatSpec extends FlatSpec with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to FreeSpec.
 */
trait ScalatraFreeSpec extends FreeSpec with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to WordSpec.
 */
trait ScalatraWordSpec extends WordSpec with ScalatraSuite

/**
 * Convenience trait to add Scalatra test support to FunSuite.
 */
trait ScalatraFunSuite extends FunSuite with ScalatraSuite
