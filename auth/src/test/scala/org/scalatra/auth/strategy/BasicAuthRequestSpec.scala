package org.scalatra
package auth
package strategy

import javax.servlet.http.HttpServletRequest

import org.scalatra.test.specs2._
import org.specs2.mock.Mockito

class BasicAuthStrategySpec extends MutableScalatraSpec with Mockito {
  "params on a request with no auth headers" should {
    val httpRequest = mock[HttpServletRequest]
    val basicAuthRequest = new BasicAuthStrategy.BasicAuthRequest(httpRequest)
    "return None" in { // https://github.com/scalatra/scalatra/issues/143
      basicAuthRequest.params must_== None
    }
  }
}
