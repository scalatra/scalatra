package org.scalatra.test

import java.util.EnumSet
import org.specs2.mutable._
import DispatcherType._

class DispatcherTypeSpec extends Specification {
  "A set of dispatcher types" should {
    "have an int value equal to the bitwise or of its members" in {
       intValue(EnumSet.of(REQUEST, INCLUDE, ASYNC)) must_== 21
    }
  }
}
