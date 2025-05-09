package org.scalatra
package auth
package strategy

import org.scalatra.ServletCompat.http.HttpServletRequest
import org.mockito.Mockito
import org.scalatra.test.specs2.*

class BasicAuthStrategySpec extends MutableScalatraSpec {
  "params on a request with no auth headers" should {
    val httpRequest      = Mockito.mock(classOf[HttpServletRequest])
    val basicAuthRequest = new BasicAuthStrategy.BasicAuthRequest(httpRequest)
    "return None" in { // https://github.com/scalatra/scalatra/issues/143
      basicAuthRequest.params must_== None
    }
  }
}
