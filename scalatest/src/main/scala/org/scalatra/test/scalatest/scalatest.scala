package org.scalatra.test
package scalatest

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatestplus.junit._
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

@RunWith(classOf[JUnitRunner])
/** Provides Scalatra test support to ScalaTest suites. The servlet tester is started before the first test in the suite
  * and stopped after the last.
  */
trait ScalatraSuite extends Suite with ScalatraTests with BeforeAndAfterAll with Matchers {
  override protected def beforeAll(): Unit = start()
  override protected def afterAll(): Unit  = stop()
}

/** Convenience trait to add Scalatra test support to JUnit3Suite.
  */
trait ScalatraJUnit3Suite extends JUnit3Suite with ScalatraSuite

/** Convenience trait to add Scalatra test support to JUnitSuite.
  */
trait ScalatraJUnitSuite extends JUnitSuite with ScalatraSuite

/** Convenience trait to add Scalatra test support to FeatureSpec.
  */
trait ScalatraFeatureSpec extends AnyFeatureSpecLike with ScalatraSuite

/** Convenience trait to add Scalatra test support to Spec.
  */
trait ScalatraSpec extends AnyFunSpecLike with ScalatraSuite

/** Convenience trait to add Scalatra test support to FlatSpec.
  */
trait ScalatraFlatSpec extends AnyFlatSpecLike with ScalatraSuite

/** Convenience trait to add Scalatra test support to FreeSpec.
  */
trait ScalatraFreeSpec extends AnyFreeSpecLike with ScalatraSuite

/** Convenience trait to add Scalatra test support to WordSpec.
  */
trait ScalatraWordSpec extends AnyWordSpecLike with ScalatraSuite

/** Convenience trait to add Scalatra test support to FunSuite.
  */
trait ScalatraFunSuite extends AnyFunSuite with ScalatraSuite
