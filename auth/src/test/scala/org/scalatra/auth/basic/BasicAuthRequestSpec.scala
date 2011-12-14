package org.scalatra
package auth
package strategy

import test.specs2._
import net.liftweb.mocks.MockHttpServletRequest

class BasicAuthStrategySpec extends MutableScalatraSpec {
  "params on a request with no auth headers" should {
    val httpRequest = new MockHttpServletRequest
    val basicAuthRequest = new BasicAuthStrategy.BasicAuthRequest(httpRequest)
    "return None" in { // https://github.com/scalatra/scalatra/issues/143
      basicAuthRequest.params must_== None
    }
  }
}
